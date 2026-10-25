#!/usr/bin/env bash
# Restores only the base batch; PITR is an explicitly reviewed separate step.
set -euo pipefail
umask 077
[[ "${1:-}" == --execute ]] || { echo 'DRY RUN: decrypt/check batch, restore to task_restore_* schema and empty isolated attachment directory. No database connection.'; exit 0; }
: "${ISOLATED_RESTORE:?set ISOLATED_RESTORE=yes on a disposable restore host}"
: "${RESTORE_DATABASE:?task_restore_* database required}"
: "${RESTORE_MYSQL_CNF:?isolated MySQL option file required}"
: "${RESTORE_DIRECTORY:?new absolute restore destination required}"
: "${BACKUP_FILE:?encrypted tar.age file required}"
: "${AGE_IDENTITY_FILE:?offline age identity required}"
[[ "$ISOLATED_RESTORE" == yes && "$RESTORE_DATABASE" =~ ^task_restore_[a-z0-9_]+$ ]] || { echo 'Isolated restore guard rejected' >&2; exit 1; }
[[ -f "$RESTORE_MYSQL_CNF" && ! -L "$RESTORE_MYSQL_CNF" && "$(stat -c '%a' "$RESTORE_MYSQL_CNF")" == 600 ]] || exit 1
[[ "$RESTORE_DIRECTORY" == /* && "$RESTORE_DIRECTORY" != / && ! -e "$RESTORE_DIRECTORY" && -f "$BACKUP_FILE" ]] || exit 1
# This destination must reside on encrypted temporary storage; never use a public web root.
mkdir -m 700 -- "$RESTORE_DIRECTORY"
age -d -i "$AGE_IDENTITY_FILE" -o "$RESTORE_DIRECTORY/batch.tar" "$BACKUP_FILE"
# Reject traversal, links, device files and unrecognized members before extraction.
python3 - "$RESTORE_DIRECTORY/batch.tar" <<'PY'
import sys,tarfile
from pathlib import PurePosixPath
with tarfile.open(sys.argv[1]) as archive:
    names=set()
    for member in archive:
        p=PurePosixPath(member.name)
        if member.name in names:
            raise SystemExit('Duplicate archive member rejected')
        names.add(member.name)
        if p.is_absolute() or '..' in p.parts or not(member.isfile() or member.isdir()) or not p.parts or p.parts[0] not in {'manifest.txt','database.sql','attachments','attachments.sha256','batch.sha256'}:
            raise SystemExit('Unsafe archive member rejected')
PY
tar --no-same-owner --no-same-permissions -C "$RESTORE_DIRECTORY" -xf "$RESTORE_DIRECTORY/batch.tar"
python3 - "$RESTORE_DIRECTORY" <<'PY'
import hashlib,re,sys
from pathlib import Path, PurePosixPath
root=Path(sys.argv[1]).resolve()
for manifest in ('batch.sha256','attachments.sha256'):
    for line in (root/manifest).read_text().splitlines():
        match=re.fullmatch(r'([0-9a-f]{64})  (.+)',line)
        if not match:
            raise SystemExit('Invalid checksum entry')
        digest,name=match.groups()
        path=PurePosixPath(name)
        allowed=name in {'database.sql','attachments.sha256'} if manifest=='batch.sha256' else (bool(path.parts) and path.parts[0]=='attachments')
        if not allowed or path.is_absolute() or '..' in path.parts:
            raise SystemExit('Unsafe checksum path')
        file=root.joinpath(*path.parts)
        if hashlib.sha256(file.read_bytes()).hexdigest()!=digest:
            raise SystemExit('Checksum mismatch: '+name)
print('Base and attachment hashes verified')
PY
# A pre-created EMPTY schema is required, and the restore identity must be restricted to it.
tables=$(mysql --defaults-extra-file="$RESTORE_MYSQL_CNF" --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$RESTORE_DATABASE'")
[[ "$tables" == 0 ]] || { echo 'Target schema is not empty' >&2; exit 1; }
mysql --defaults-extra-file="$RESTORE_MYSQL_CNF" "$RESTORE_DATABASE" < "$RESTORE_DIRECTORY/database.sql"
echo 'Base import and attachment checks complete. Keep workers/webhooks disabled; perform PITR and business reconciliation before sign-off.'
