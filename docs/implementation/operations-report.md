# Operations implementation report

Date:2026-10-24. Scope:CDEX-003/013/016/038–042 operational source/templates and acceptance evidence. Status:**implemented, partially statically checked; production acceptance pending**. No production access, database connection, service installation or external publication was performed.

Implemented: nonroot hardened Linux systemd JAR unit; same-origin HTTPS Nginx with fixed trusted proxy headers/unknown-Host rejection/CSP/10MB application and11MB edge limits/private Actuator; runtime versus migration environment; MySQL8.4 runtime DML-only versus migration DDL/backup identity templates. Production preflight rejects missing required env, non-MySQL/unverified TLS/root/migration runtime accounts, wrong storage path, missing base64 32byte TOTP key, runtime migrations and insecure listener/cookie.

Executable scripts: start-demo.ps1 requires explicit -Demo, validates Java21 and Node22>=22.23.4 or Node24>=24.22.1 for local demo use and free configurable ports, builds/reuses the JAR, pins isolated local H2/storage and hides java+Vite child windows; stop-demo.ps1 checks PID/exact UTC creation time/executable before stopping trees. The launcher removes inherited Spring configuration overrides with a true null and waits for readiness after account seeding; DPAPI uses the native .NET API. Bootstrap passwords/TOTP keys are never logged or persisted in process state. verify.ps1 invokes Maven plus npm checks; security-http-check.ps1 validates HTTPS response header presence; backup.sh defaults dry-run and quiesces the app for a shared encrypted DB+attachment batch; archive-binlog.sh requires a confirmed closed log; archive-attachments.sh archives immutable objects for post-base PITR; restore-isolated.sh defaults dry-run, restricts schema names/empty targets, rejects unsafe archives, validates hashes and restores the base only. Runbooks explicitly separate base restoration from manually reviewed isolated original-schema binlog replay and require target-point attachment reconciliation. No numerical RPO/RTO or retention policy was invented.

CI source: Maven verify; frontend ci/lint/typecheck/test/build; redacted Gitleaks, Semgrep, Trivy, OWASP Dependency-Check CVSS>=7, npm audit high; Maven/npm CycloneDX SBOM generation. Tools/images/rules/feed availability and scans are not claimed verified. Failures are gates, not clean-scan evidence. Reviewed action SHAs/container digests and protected-branch rules remain release work.

Docs: source-derived OpenAPI draft87operations/68paths with explicit DTO/implementation gaps; README launch/check commands, development, ADR-001 candidate proof conditions, user/admin handover, deployment/rollback, monitoring, backup/PITR, security baseline, release manifest/checklist and all42-task P0/P1 acceptance ledger with19 mandatory regression scenarios plus supplemental cases. Business defaults and pending ADR register remain in force. Initial shell Maven DNS failure has since been bypassed by root's sanctioned dependency bridge; final candidate outcomes require integration evidence.

## Actual checks

| Check | Actual result |
|---|---|
| PowerShell Parser.ParseFile for all demo-related PowerShell scripts, including start-demo.ps1 and stop-demo.ps1 | PASS, zero parser errors |
| Demo safety guards | PASS: missing -Demo and occupied-port attempts rejected before starting any child |
| Local H2 demo/runtime probes | PASS: Windows PowerShell5.1 and PowerShell7 environment restoration and real employee login; exact process identity rejection cases; live health/CSRF/session/logout and cached-Chromium login flow. Local synthetic evidence only |
| Primary Python scripts/check-operations.py | PASS:42 unique ordered tasks,19 mandatory scenarios, required edge/service/env controls, four encryption/dry-run guards and LF text |
| Python compile of embedded shell Python blocks | PASS:one production env block and two restore validation/hash blocks |
| E:/msys64/usr/bin/bash.exe -c shell syntax loop | NOT RUN:MSYS process fails before shell starts with NtCreateDirectoryObject0xC0000022; --version also fails; no Bash syntax success claimed |
| WSL availability | unavailable; no Linux installation attempted |
| PyYAML import | unavailable; cached SnakeYAML alternative attempted separately |
| SnakeYAML2.5 SafeConstructor with duplicate keys disabled, explicit local JDK21.0.2 source launcher | PASS: CI YAML and docs/api/openapi.yaml parsed; this local JDK is not an approved deployment patch |
| OpenAPI source-draft structural validation | PASS:68paths/87operations, unique operation IDs, all path parameters declared, internal references resolve; not full OpenAPI semantic or HTTP conformance validation |
| systemd-analyze/nginx -t/TLS/DB account denial/backup restore drills | NOT RUN, requires isolated Linux/MySQL8.4 environment |
| CI scanners and SBOM commands | NOT RUN locally; executable workflow and tools documented |

## Remaining acceptance gates

Candidate Boot3.5/4.1 compatibility and exact patch freeze, supported patched Java21 deployment, blank/historical MySQL8.4 migration, real session/TOTP/tenant security regression, scanner behavior, E2E browser/WeChat/width/accessibility,50user performance, Linux clean-host deployment and validated same-batch+PITR restore still need recorded evidence. All42 rows began Pending; operational rows now show Partial evidence only for demonstrated source/static scope. Local checks do not establish release approval.
