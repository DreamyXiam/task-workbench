"""Read-only operational template consistency checks; no services or DB access."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
errors = []
def check(condition, message):
    if not condition:
        errors.append(message)

ledger = (root / "docs/testing/acceptance-matrix.md").read_text(encoding="utf-8")
ids = re.findall(r"^\| CDEX-(\d{3}) \|", ledger, re.M)
check(ids == [f"{i:03}" for i in range(1,43)], "Acceptance ledger must contain every task exactly once")
for case in [*[f"A-{i:02}" for i in range(1,8)],*[f"B-{i:02}" for i in range(1,6)],*[f"K-{i:02}" for i in range(1,5)],*[f"O-{i:02}" for i in range(1,4)]]:
    check(f"| {case} |" in ledger, "Missing mandatory scenario " + case)
nginx = (root / "deploy/nginx/task-workbench.conf").read_text(encoding="utf-8")
for token in ["ssl_reject_handshake on", "TLSv1.2 TLSv1.3", "client_max_body_size 11m", "frame-ancestors 'none'", "proxy_set_header X-Forwarded-For $remote_addr", "location ^~ /actuator"]:
    check(token in nginx, "Missing edge control " + token)
unit = (root / "deploy/systemd/task-workbench.service").read_text(encoding="utf-8")
for token in ["User=task-workbench", "NoNewPrivileges=true", "ProtectSystem=strict", "CapabilityBoundingSet=", "UMask=0077"]:
    check(token in unit, "Missing service control " + token)
env = (root / "deploy/env/prod.env.example").read_text(encoding="utf-8")
for token in ["TASK_FLYWAY_ENABLED=false", "TASK_SECURE_COOKIE=true", "TASK_TOTP_ENCRYPTION_KEY=", "TASK_DB_PASSWORD=", "MAX_FILE_SIZE=10MB"]:
    check(token in env, "Missing production configuration " + token)
check("TASK_MIGRATION_PASSWORD" not in env, "Migration credentials must not reach runtime")
for name in ["backup.sh", "archive-binlog.sh", "archive-attachments.sh", "restore-isolated.sh"]:
    text = (root / "scripts" / name).read_text(encoding="utf-8")
    check('"${1:-}" == --execute' in text, "Missing dry-run guard " + name)
    check("set -euo pipefail" in text, "Missing shell failure guard " + name)
    check("age " in text, "Missing encryption/decryption " + name)
for path in [root/"README.md", *root.glob("deploy/**/*"), *root.glob("scripts/*"), *root.glob("docs/operations/*.md")]:
    if path.is_file() and path.suffix in {".md",".example",".template",".sh",".ps1",".conf",".service",".py"}:
        check(b"\r\n" not in path.read_bytes(), "Use portable LF line endings: " + str(path))
if errors:
    print("\n".join(errors), file=sys.stderr)
    raise SystemExit(1)
print("PASS: 42 task rows; 19 mandatory scenarios; edge/service/env controls; four dry-run encryption guards; portable text")
