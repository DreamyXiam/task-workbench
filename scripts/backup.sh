#!/usr/bin/env bash
# Dry-run is default. Execute only on the approved host during a maintenance window.
set -euo pipefail
umask 077
[[ "${1:-}" == --execute ]] || { printf '%s\n' 'DRY RUN: stop app; dump MySQL; hash attachments; encrypt batch with age; restart app. No commands executed.'; exit 0; }
: "${BACKUP_APPROVED:?set BACKUP_APPROVED=yes after maintenance approval}"
[[ "$BACKUP_APPROVED" == yes && "$EUID" -eq 0 ]] || { echo 'Root and approved window required' >&2; exit 1; }
: "${MYSQL_BACKUP_CNF:?root-owned MySQL client option file required}"
: "${AGE_RECIPIENT:?age public recipient required}"
: "${BACKUP_ROOT:?absolute encrypted backup directory required}"
[[ "$BACKUP_ROOT" == /* && "$BACKUP_ROOT" != / && -d "$BACKUP_ROOT" ]] || exit 1
BACKUP_ROOT=$(realpath -e -- "$BACKUP_ROOT")
[[ "$BACKUP_ROOT" != /var/lib/task-workbench/attachments* ]] || { echo 'Backup directory cannot reside inside attachment source' >&2; exit 1; }
[[ -f "$MYSQL_BACKUP_CNF" && ! -L "$MYSQL_BACKUP_CNF" && "$(stat -c '%a:%u' "$MYSQL_BACKUP_CNF")" == 600:0 ]] || { echo 'Client file must be root-owned 0600 regular file' >&2; exit 1; }
for tool in mysql mysqldump age tar sha256sum systemctl flock; do command -v "$tool" >/dev/null; done
exec 9>/run/lock/task-workbench-backup.lock
flock -n 9 || { echo 'Another backup is active' >&2; exit 1; }
batch="$(date -u +%Y%m%dT%H%M%SZ)-$(cat /proc/sys/kernel/random/uuid)"
work=$(mktemp -d /var/lib/task-workbench/backup-work.XXXXXX)
restart_required=false
cleanup() {
  code=$?
  trap - EXIT
  if $restart_required; then systemctl start task-workbench.service || code=1; fi
  # Exact known temporary directory only, no recursive shell-built deletion.
  if [[ "$work" == /var/lib/task-workbench/backup-work.* ]]; then rm -rf -- "$work"; fi
  exit "$code"
}
trap cleanup EXIT
systemctl is-active --quiet task-workbench.service || { echo 'App must initially be active' >&2; exit 1; }
restart_required=true
systemctl stop task-workbench.service
! systemctl is-active --quiet task-workbench.service
# All application writers/scheduled jobs are stopped. No migration or external writer is allowed.
# Application attachments are immutable objects; replacement writes must use new versions.
printf 'batch=%s\ncreated_utc=%s\nconsistency=all-writers-stopped\n' "$batch" "$(date -u +%FT%TZ)" > "$work/manifest.txt"
mysql --defaults-extra-file="$MYSQL_BACKUP_CNF" --batch --skip-column-names -e 'SELECT VERSION()' >> "$work/manifest.txt"
# Includes source binlog filename/position in SQL; --no-tablespaces avoids PROCESS privilege.
mysqldump --defaults-extra-file="$MYSQL_BACKUP_CNF" --single-transaction --source-data=2 --no-tablespaces --hex-blob --triggers --events --set-gtid-purged=OFF task_workbench > "$work/database.sql"
[[ -d /var/lib/task-workbench/attachments && ! -L /var/lib/task-workbench/attachments ]] || { echo 'Attachment root missing or symlinked' >&2; exit 1; }
[[ -z "$(find /var/lib/task-workbench/attachments -type l -print -quit)" ]] || { echo 'Attachment symlinks rejected' >&2; exit 1; }
cp -a /var/lib/task-workbench/attachments "$work/attachments"
(cd "$work" && find attachments -type f -print0 | sort -z | xargs -0 -r sha256sum -- > attachments.sha256)
(cd "$work" && sha256sum database.sql attachments.sha256 > batch.sha256)
tar -C "$work" -cf - manifest.txt database.sql attachments attachments.sha256 batch.sha256 | age -r "$AGE_RECIPIENT" -o "$BACKUP_ROOT/$batch.tar.age.partial"
mv -- "$BACKUP_ROOT/$batch.tar.age.partial" "$BACKUP_ROOT/$batch.tar.age"
sha256sum "$BACKUP_ROOT/$batch.tar.age" > "$BACKUP_ROOT/$batch.tar.age.sha256"
printf 'Encrypted batch complete: %s\n' "$batch"
# Copy encrypted files and checksum to offline/off-host storage; verify receipt separately.
