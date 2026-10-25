# Development

This is a new V1.5 source project alongside the preserved requirements/audit documents. Main product source: backend/ and frontend/. Never edit a released Flyway migration; append a new version. No production databases are used by local verification.

Prerequisites: supported patched Java21, Maven3.9+, Node24 LTS/npm, optional isolated MySQL8.4 for integration. DNS/network is required for a clean dependency fetch. The default POM is the proposed V1.5 candidate; see [ADR-001](adr/ADR-001-tech-stack.md). Never release a JAR produced with offline-verification.

From repository root (PowerShell):

```powershell
mvn -B -ntp -f backend/pom.xml verify
Set-Location frontend
npm ci
npm run lint
npm run typecheck
npm test
npm run build
Set-Location ..
```

The combined equivalent is `./scripts/verify.ps1`. For a machine containing only legacy Maven cache, `./scripts/verify.ps1 -OfflineBackend` adds `-o -Poffline-verification`; the output tests code under that older dependency graph only. It does not close CDEX-002 or CDEX-038. A clean online candidate build remains mandatory.

Use a local/demo profile only for synthetic fixtures. Read backend configuration before setting variables; Spring does not auto-load .env files. Set a unique TASK_BOOTSTRAP_PASSWORD in the process, never in source or command history. Local HTTP cookies are permitted only in the explicitly selected demo profile. Production must reject missing credentials, use HTTPS secure cookies, loopback listeners, MySQL and an external TOTP encryption key. See README for the implemented demo launch command.

Write APIs carry CSRF obtained from /api/v1/auth/csrf, the session cookie, and UUID Idempotency-Key (except auth). Object authorization must use active role/company scope independently of client-provided IDs. UI allowedActions improves interaction, while the server enforces permission and state. Test boundaries, concurrent state conflicts, immutable historical facts and denied access. Use synthetic data exclusively.

Before review: inspect changed migrations/API contract, run relevant tests, record actual commands/results, attach SBOM/security evidence. No success badge or complete acceptance status without a report. [Acceptance matrix](testing/acceptance-matrix.md) distinguishes local evidence from release acceptance.
