#!/usr/bin/env bash
# Archives an operator-identified CLOSED binlog. Never purge source automatically.
set -euo pipefail
umask 077
[[ "${1:-}" == --execute ]] || { echo 'DRY RUN: encrypt a closed MySQL binlog; no database commands.'; exit 0; }
: "${CLOSED_BINLOG:?absolute closed binlog path required}"
: "${BINLOG_CONFIRMED_CLOSED:?set to yes after checking SHOW BINARY LOG STATUS}"
: "${AGE_RECIPIENT:?age public recipient required}"
: "${BINLOG_ARCHIVE_ROOT:?existing encrypted archive directory required}"
[[ "$BINLOG_CONFIRMED_CLOSED" == yes && "$CLOSED_BINLOG" == /* && -f "$CLOSED_BINLOG" && ! -L "$CLOSED_BINLOG" && -d "$BINLOG_ARCHIVE_ROOT" ]] || exit 1
name=$(basename -- "$CLOSED_BINLOG")
[[ "$name" =~ ^mysql-bin\.[0-9]{6,}$ ]] || { echo 'Unexpected binlog filename' >&2; exit 1; }
[[ ! -e "$BINLOG_ARCHIVE_ROOT/$name.age" ]] || { echo 'Archive exists; refusing overwrite' >&2; exit 1; }
# mysqlbinlog validates format/checksum before encryption; no SQL execution.
mysqlbinlog --verify-binlog-checksum "$CLOSED_BINLOG" >/dev/null
age -r "$AGE_RECIPIENT" -o "$BINLOG_ARCHIVE_ROOT/$name.age.partial" "$CLOSED_BINLOG"
mv -- "$BINLOG_ARCHIVE_ROOT/$name.age.partial" "$BINLOG_ARCHIVE_ROOT/$name.age"
sha256sum "$BINLOG_ARCHIVE_ROOT/$name.age" > "$BINLOG_ARCHIVE_ROOT/$name.age.sha256"
