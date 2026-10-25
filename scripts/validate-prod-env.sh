#!/usr/bin/env bash
set -euo pipefail
fail() { printf '%s\n' "$1" >&2; exit 1; }
[[ "${SPRING_PROFILES_ACTIVE:-}" == prod ]] || fail 'prod profile required'
for name in TASK_DB_URL TASK_DB_USER TASK_DB_PASSWORD TASK_STORAGE_ROOT TASK_TOTP_ENCRYPTION_KEY; do
  [[ -n "${!name:-}" ]] || fail "Required environment missing: $name"
done
[[ "$TASK_DB_URL" == jdbc:mysql:* ]] || fail 'Production requires MySQL'
[[ "$TASK_DB_URL" == *sslMode=VERIFY_IDENTITY* ]] || fail 'MySQL certificate verification required'
[[ "$TASK_DB_USER" != root && "$TASK_DB_USER" != task_migrate ]] || fail 'Runtime account must have no DDL privilege'
[[ "${TASK_FLYWAY_ENABLED:-}" == false ]] || fail 'Runtime Flyway must be disabled'
[[ "$TASK_STORAGE_ROOT" == /var/lib/task-workbench/attachments ]] || fail 'Unexpected production storage root'
python3 - <<'PY'
import base64, os
try:
    assert len(base64.b64decode(os.environ['TASK_TOTP_ENCRYPTION_KEY'], validate=True)) == 32
except Exception:
    raise SystemExit('TOTP key must be base64 encoding of exactly32 random bytes')
PY
[[ "${SERVER_ADDRESS:-}" == 127.0.0.1 && "${SERVER_SERVLET_SESSION_COOKIE_SECURE:-}" == true ]] || fail 'Loopback binding and secure cookie required'
java_version=$(/usr/bin/java -version 2>&1 | head -n 1)
[[ "$java_version" == *'"21.'* ]] || fail 'Java 21 required; verify security patch against release manifest'
printf '%s\n' 'Production environment structure valid; release patch approval is still required.'
