# V1.5 acceptance matrix

Status date:2026-10-25. **No row is release-accepted.** Pending means the requirement has not yet been demonstrated end to end on the approved V1.5 stack; code may exist. A local offline test/build may provide partial evidence without closing the row. Use [requirements](../requirements/v15-tasks.md) as the full acceptance definition; this ledger does not replace it. Every applicable P0 must pass; each incomplete P1 needs explicit business disposition.

States: Pending / Partial evidence / Blocked / Verified (named environment and report) / Accepted (release sign-off). A summary saying tests passed cannot verify untested scenarios. Reports must include command, exit code, runtime/dependencies, test count/result, data profile and remaining gaps. CDEX-002 selects a locally tested3.5 baseline with remaining release gates; CDEX-038/040/041/042 cannot close on static templates.

| Task | Priority | Requirement | Owner | Release status | Evidence to review |
|---|---|---|---|---|---|
| CDEX-001 | P0 | Repository and V1.4 mapping | integration | Partial evidence | [Original audit](../audit/v14-gap-analysis.md); provenance/reconciliation remains pending |
| CDEX-002 | P0 | Candidate compatibility PoC | integration | Partial evidence | [ADR-001](../adr/ADR-001-tech-stack.md): both candidates attempted; final release gates remain |
| CDEX-003 | P0 | Build and CI gates | operations + integration | Partial evidence | [operations-report.md](../implementation/operations-report.md); [frontend clean-build evidence](evidence/frontend-clean-build.json) |
| CDEX-004 | P0 | API contract/errors | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-005 | P0 | Original schema migration | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [MySQL V1-V9 engine evidence](evidence/mysql-v9-engine.json) |
| CDEX-006 | P0 | Missing business entities | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-007 | P0 | Role and company membership | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-008 | P0 | Object authorization and isolation | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-009 | P1 | Delegation and handover | backend | Partial evidence | [organization-changes-report.md](../implementation/organization-changes-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-010 | P0 | Constraints/indexes/versions | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [MySQL V1-V9 engine evidence](evidence/mysql-v9-engine.json) |
| CDEX-011 | P0 | Session/CSRF/revocation | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-012 | P0 | TOTP and step-up | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-013 | P0 | HTTPS and security headers | operations + integration | Partial evidence | [operations-report.md](../implementation/operations-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-014 | P0 | Attachments and safe export | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-015 | P0 | Audit/keys/retention | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-016 | P0 | Security regression gates | operations + integration | Partial evidence | [operations-report.md](../implementation/operations-report.md); [frontend clean-build evidence](evidence/frontend-clean-build.json); backend/current online scans pending |
| CDEX-017 | P0 | Task/subtask state machine | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-018 | P0 | Deadline history and extensions | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-019 | P0 | Idempotency and concurrency | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-020 | P0 | Create/confirm/cross-company consent | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-021 | P0 | Receive/assign/submit/rework/accept | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-022 | P1 | Versioned explainable matching | backend | Partial evidence | [matching-report.md](../implementation/matching-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-023 | P1 | Human correction and statistics | backend | Partial evidence | [matching-report.md](../implementation/matching-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-024 | P0 | Transactional outbox delivery | backend | Partial evidence | [wecom-report.md](../implementation/wecom-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-025 | P0 | Durable idempotent scheduled jobs | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-026 | P0 | KPI golden formulas | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-027 | P1 | Complexity/calendar simulation | backend | Partial evidence | [calendar-report.md](../implementation/calendar-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-028 | P0 | Monthly immutable snapshots | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-029 | P0 | Appeals/adjustment/lock | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-030 | P1 | Scorecards/reports/export | backend | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-031 | P0 | Vue/TS/Pinia tokens | frontend | Partial evidence | [frontend-report.md](../implementation/frontend-report.md); [frontend clean-build evidence](evidence/frontend-clean-build.json) |
| CDEX-032 | P0 | Login/TOTP/watermark | frontend | Partial evidence | [frontend-report.md](../implementation/frontend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-033 | P0 | Role dashboard/list/notifications | frontend | Partial evidence | [frontend-report.md](../implementation/frontend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-034 | P0 | Create/matching UI | frontend | Partial evidence | [frontend-report.md](../implementation/frontend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-035 | P0 | Detail/timeline/execution UI | frontend | Partial evidence | [frontend-report.md](../implementation/frontend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-036 | P1 | KPI/appeal UI | frontend | Partial evidence | [frontend-report.md](../implementation/frontend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-037 | P1 | Responsive/WeChat/accessibility | frontend | Partial evidence | [frontend-report.md](../implementation/frontend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-038 | P0 | Secure deployment | operations + integration | Partial evidence | [operations-report.md](../implementation/operations-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-039 | P0 | Health/metrics/alerts | operations + integration | Partial evidence | [operations-report.md](../implementation/operations-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-040 | P0 | Backup/binlog/attachment restore | operations + integration | Partial evidence | [operations-report.md](../implementation/operations-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-041 | P0 | Integrated/performance acceptance | integration | Partial evidence | [backend-report.md](../implementation/backend-report.md); [checkpoint](../implementation/build-report.md) |
| CDEX-042 | P0 | Release and handover | operations + integration | Partial evidence | [operations-report.md](../implementation/operations-report.md); [checkpoint](../implementation/build-report.md) |

## Must-test regression matrix

The rows below define required release scenarios. Local evidence is summarized in [build-report](../implementation/build-report.md); they remain pending for full release acceptance until each complete scenario is proved in its required environment. A case may be automated locally while its real-environment acceptance remains pending.

| Case | Tasks | Test and pass condition | Required environment |
|---|---|---|---|
| A-01 |008/014/016|A-company guessed B task/subtask/file/export/stat IDs disclose no body or metadata|API integration, MySQL8.4|
| A-02 |008/020|Unaccepted cross-company invitation exposes only necessary request fields, no execution task|API integration|
| A-03 |007–009|Switch active multi-company role narrows scope; no union of unrelated privileges|API + browser|
| A-04 |011/012|Password reset/account disable/role change invalidates existing cookies and requires appropriate reauth|API + browser|
| A-05 |011/016|Missing/wrong CSRF token rejects write without changing business data|API integration|
| A-06 |014/016|Invalid executable/MIME/magic/ZIP traversal/bomb rejected; scan outage remains quarantined|Scanner + filesystem integration|
| A-07 |014/030|All =,+,-,@ formula-leading cells exported as text; exporter/time watermark and audit|Spreadsheet application|
| B-01 |017|Overdue task submitted on time remains historically overdue; current and historical fields coexist|Deterministic clock tests|
| B-02 |019/021|Concurrent acceptance/rework permits one legal winner and explicit409 loser|Real transactional MySQL|
| B-03 |019/020|Same actor/key/request replay has one task/subtask/outbox effect; changed body conflicts|Concurrent API/MySQL|
| B-04 |017/021|Rework reopens permitted aggregate and never loses submitted history|Domain + API|
| B-05 |024/025|Delivery failure/restart preserves committed task and recoverable outbox without duplicate business effects|Fault injection|
| K-01 |026|100/80/100/83.33/50 indicators with30/25/20/10/15 total about85.83 using defined rounding|Golden unit cases|
| K-02 |026/027|Zero denominator is explicit N/A, not silent0/100; same template snapshot reproducible|Unit + DB snapshot|
| K-03 |028/029|Later task/deadline changes do not silently alter frozen raw scores or source snapshot|MySQL + historical fixtures|
| K-04 |029|Day8 and unauthorized adjustment API denied; authorized correction retains original score+audit|Clock + API + TOTP|
| O-01 |025/028|Month boundary/restart/repeated trigger makes one formal run per company/period/template|Clock + MySQL fault injection|
| O-02 |040|Same-batch database/attachment and later-point PITR restore reconciles object hashes, task/KPI relations|Isolated MySQL8.4 restore|
| O-03 |041|50users p95 API<1s and submission<2s, stated hardware/data/load/duration/error rate|Production-like load test|
| X-01 |002/003|Both candidate stacks attempted; supported combination clean-builds and migrates with Session/CSRF/Mapper|Online clean build/MySQL|
| X-02 |012/029|TOTP replay, recovery-code reuse, brute force, expiry and adjustment/export/unlock step-up denied|Security integration|
| X-03 |013/038|TLS, spoofed Host/forwarded headers, secure cookie, CSP errors, upload limits, inaccessible Actuator|Linux HTTPS + real browser|
| X-04 |037|375/390/768/1024/1440 without horizontal overflow; keyboard/focus/AA, WeChat auth/download|Browser + real WeChat devices|
| X-05 |005/010|Blank/historical migrations, tenant FK constraints, safe sort and EXPLAIN indexes|Exact MySQL8.4 release|
| X-06 |015/016|No secrets/history tampering; current scans fail high/critical, full license+SBOM reviewed|CI + immutable audit target|

## Original V1.4 acceptance reconciliation

The13 original scenarios require a readable source and explicit original-ID/page-to-case mapping; see audit gap analysis. They are not invented or marked passed here. The end-to-end business pilot must cover create→matching→confirm→receive→assign→submit→summarize→accept→monthly assessment, plus refusal/cancellation/rework/objection paths. Record scope gaps as defects rather than deleting their acceptance rows.

## Evidence disposition

- Initial shell Maven DNS failure: subsequently bypassed by sanctioned dependency bridge; wait for actual candidate build/test reports, not compatibility approval.
- Offline cached Boot3.3 tests: partial code evidence only; cannot close CDEX-002 or production readiness.
- Script/parser/text checks: local static evidence only, not Linux deployment, TLS, DB privileges or restore proof.
- Real security scanners, MySQL migration/concurrency, E2E browser/device, load and recovery drills require independent execution and retained reports.

Local operations disposition: CDEX-003/013/016/038/039/040/042 have partial evidence from templates, documentation and read-only static checks in operations-report.md. This does not assert CI scan, Linux installation, real alerting, DB restore or final release acceptance. Other owner reports will be incorporated after their actual results are available.

##2026-10-25 checkpoint and pause

The user requested that the current appointments/delegation/handover, calendar simulation and actual Excel/PDF export part be completed and then paused. Dedicated reports describe implementation and test scope. No new production trial, deployment, scan, load or recovery task is authorized by this checkpoint note.

Earlier real MySQL8.4.13 V1-V5 migration +13HTTP checks and35synthetic API browser viewport checks passed on2026-10-24. New V8/V9 and45browser combinations must not inherit those results. On2026-10-25 the exact V1-V9 SQL files subsequently executed successfully on a fresh isolated MySQL8.4.13 engine, and a new-directory npm ci/typecheck/lint/20-test/build gate passed. These are partial evidence only: localhost client channels remained denied, so Flyway/JDBC/HTTP did not run against that database; Vite loopback and Edge debugging pipes also remained blocked, so the45-case browser fixture did not run. Frontend offline audit/SBOM evidence exists, while current backend scanning/SBOM and all release-environment gates remain pending.
