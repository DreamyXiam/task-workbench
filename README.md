# 任务工作台 V1.5

A source project for the task lifecycle, scoped Session/CSRF security, immutable KPI assessment and a Vue3/TypeScript workbench. This working tree is a reviewable implementation; it is **not yet approved for production**. Check [acceptance](docs/testing/acceptance-matrix.md), [implementation evidence](docs/implementation/) and [release gates](docs/release/v1.5-release-checklist.md) for actual scope and verification limits.

Prerequisites: patched Java21, Maven3.9+, Node24 LTS/npm. Deployment requires MySQL8.4 LTS, Linux/systemd/Nginx and approved TLS/backup infrastructure. The tested local baseline is recorded in [ADR-001](docs/adr/ADR-001-tech-stack.md). Production acceptance remains separate.

From repository root, build and check:

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

`./scripts/verify.ps1` runs these same checks. `-Offline` uses only already cached dependencies without changing their versions.

For a local synthetic-data review, first install frontend dependencies with `npm ci` as above. Then use the explicit H2 demo launcher (replace the executable paths with your installed patched Java21/Node24 runtimes):

```powershell
./scripts/start-demo.ps1 -Demo -JavaPath 'C:\your-jdk-21\bin\java.exe' -NodePath 'C:\your-node-24\node.exe' -MavenPath 'C:\your-maven\bin\mvn.cmd'
```

With dependencies already installed, the local H2 demo also accepts Node22.23.4 or later in the Node22 line. Node24.22.1 or later in the Node24 line remains supported; the package engine, clean dependency installation and production baseline remain Node24. The installed Java21.0.2/Node22.23.4 executables were verified for this local demo only.

The launcher builds the backend, securely prompts for a unique bootstrap password, generates a demo-only TOTP key if absent and protects its local copy with Windows DPAPI, and starts both processes in the background. It validates that the selected ports are free, binds to loopback and pins the database/attachments under `.task-storage/demo-<backend-port>/`; inherited database settings cannot redirect it to MySQL. It reports URLs, PIDs, log paths and the exact stop command without printing credentials. It installs no runtimes, services or dependencies. `-UseBuiltArtifacts` reuses an existing JAR after you have built/verified it. Use `-BackendPort 18080 -FrontendPort 15173` if the defaults8080/5173 are occupied; Vite targets the selected backend port.

The launcher removes inherited Spring configuration overrides rather than passing empty environment values, then restores the caller's environment, including absent values. Health readiness waits for account initialization before reporting the demo ready. Startup/configuration and process-identity regression probes are in `scripts/tests/`; process termination requires the exact executable and UTC creation timestamp in both Windows PowerShell5.1 and PowerShell7.

Open the reported frontend URL. The empty demo database seeds `demo.leader`, `demo.head`, `demo.employee`, `demo.finance`, `demo.other`, `demo.group` and `demo.admin` using the bootstrap password you entered. Every account must change it on first login; leadership/admin accounts then enroll TOTP. No fixed password is shipped. Stop using the reported `./scripts/stop-demo.ps1 -StateFile '<reported-path>'` command, which checks process identity before terminating the demo process trees. Restarts on the same Windows user/machine reuse the DPAPI-protected demo key. Do not copy a demo database to another machine without its privately preserved original key. A conflicting key is rejected. Keys and passwords are never written to the PID state file.

Production has a separate configuration: external MySQL credentials/key, Secure cookies, fixed HTTPS same origin, restricted files and no runtime migration privilege. `.env.example` documents names; it is not automatically loaded. Review [deployment/rollback](docs/operations/deployment.md), [backup/PITR](docs/operations/backup-restore.md), [monitoring](docs/operations/monitoring.md), [security](docs/security/security-baseline.md), [development](docs/development.md) and [user/admin guide](docs/operations/user-admin-guide.md). Scripts default to dry-run; they have not touched a real database or production service.

The preserved business baseline is one host department,0–2 collaborators, explicit cross-company agreement, five original KPI weights30/25/20/10/15,7day objections and personal KPI disabled. [API draft and limits](docs/api/contract-notes.md), [OpenAPI](docs/api/openapi.yaml), [requirements](docs/requirements/v15-tasks.md) and [ADR register](docs/adr/README.md) retain pending business decisions. New formulas/policies are simulations until approved.
