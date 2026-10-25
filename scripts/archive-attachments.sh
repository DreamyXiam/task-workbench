#!/usr/bin/env bash
# Frequent immutable-object snapshots complement binlogs. No deletion/pruning.
set -euo pipefail
umask 077
[[ "${1:-}" == --execute ]] || { echo 'DRY RUN: encrypt immutable attachment objects for post-base PITR; no database connection.'; exit 0; }
: "${IMMUTABLE_OBJECTS_CONFIRMED:?set yes only after verifying atomic immutable object writes}"
: "${AGE_RECIPIENT:?age public recipient required}"
: "${ATTACHMENT_ARCHIVE_ROOT:?existing encrypted archive directory required}"
[[ "$IMMUTABLE_OBJECTS_CONFIRMED" == yes && "$ATTACHMENT_ARCHIVE_ROOT" == /* && -d "$ATTACHMENT_ARCHIVE_ROOT" ]] || exit 1
ATTACHMENT_ARCHIVE_ROOT=$(realpath -e -- "$ATTACHMENT_ARCHIVE_ROOT")
[[ "$ATTACHMENT_ARCHIVE_ROOT" != /var/lib/task-workbench/attachments* ]] || { echo 'Archive must be outside attachment source' >&2; exit 1; }
exec 9>/run/lock/task-workbench-object-archive.lock
flock -n 9 || exit 1
batch="$(date -u +%Y%m%dT%H%M%SZ)-$(cat /proc/sys/kernel/random/uuid)"
# Full snapshots trade storage for a simple auditable object archive. Changed files make tar fail.
tar -C /var/lib/task-workbench -cf - attachments | age -r "$AGE_RECIPIENT" -o "$ATTACHMENT_ARCHIVE_ROOT/objects-$batch.tar.age.partial"
mv -- "$ATTACHMENT_ARCHIVE_ROOT/objects-$batch.tar.age.partial" "$ATTACHMENT_ARCHIVE_ROOT/objects-$batch.tar.age"
sha256sum "$ATTACHMENT_ARCHIVE_ROOT/objects-$batch.tar.age" > "$ATTACHMENT_ARCHIVE_ROOT/objects-$batch.tar.age.sha256"
echo "Encrypted objects snapshot complete: $batch. Off-host copy and DB reference/hash reconciliation remain required."
