# Backend implementation checkpoint

Updated2026-10-25. This is a new isolated source project; no historical production database or existing business repository was migrated. The source V1.4 local rendering has readability/provenance limits recorded in the audit. Code coverage and requirement acceptance are different claims.

## Implemented scope

- Modular Spring Boot/MyBatis application under `/api/v1`, unified JSON envelopes/errors/trace IDs, explicit state-changing endpoints, Session cookies and CSRF, mandatory initial password change, account lockout, TOTP and one-time recovery codes, fresh TOTP for privileged configuration/KPI/export, revocable sessions and explicit role context.
- Company/department/user scope CRUD with resource authorization, no implicit business visibility for administrators, minimal cross-company invitation/directory data, employee assignment visibility, versioned writes and idempotency keys. Scoped admins cannot edit global or shared foreign accounts or grant global leadership.
- Task create/match/confirm, one host and0-2collaborators, foreign-company consent, receive/assign/progress/submit/withdraw, immutable result history, host summaries, explicit gaps, targeted rework, acceptance/cancellation, deadline applications/decisions/history and independent historical overdue facts.
- Transactional outbox, station and WeCom delivery records with retries/leases/fencing, durable scheduled execution keys, missed-month catch-up, immutable monthly inputs/template versions/raw scores, N/A denominators,7day objections, appeals, reasoned adjustments and temporary audited unlocks.
- Attachments outside public web roots, random keys, signature/MIME/extension/size and legacy DOC/XLS/ZIP validation, traversal/bomb limits, fail-closed quarantine/scanning, per-read authorization, no storage keys in serialized views. Original whitelist retained; modern Office formats await business decision.
- Explainable fixed/tag matching, negative words, stable ties, global/company precedence, stored input/rule snapshots and human adoption feedback. See matching-report.
- Actual XLSX inline-text export and PDFBox PDF from the same authorized score snapshot; actor/scope/time watermark, text-only formula-like cells, fresh TOTP, user/concurrency limits,5000row/200page bounds. CSV compatibility retained. PDF needs a trusted Chinese TTF via TASK_EXPORT_FONT; absence fails explicitly.
- Current checkpoint adds scoped delegation, appointments/handover and calendar/complexity simulation. Their dedicated reports and final tests define actual status; no candidate policy silently changes official scores.

## Verified before the checkpoint additions

- Java21.0.13/Boot3.5.16/MyBatis3.0.5/Flyway11.20.3:69tests,0failures/errors/skips. Includes21matching and12WeCom tests.
- Export focused tests:6passed,0failures/errors/skips (XLSX formula/XML safety, PDF numbers/watermark/pagination, export size, TOTP, employee denial and rate limiting).
- MySQL8.4.13:13live HTTP checks passed against an intermediate V1-V5 build on2026-10-24: login/CSRF, initial password change/TOTP, company number, idempotent replay/body mismatch, foreign guessed ID denial, receive/submit/summary, withdrawal, rework/re-summary/acceptance and logout revocation.
- The live smoke script now includes concurrent same-key writes and concurrent acceptance. Those added live cases have not passed merely because the older13did. Final checkpoint results are recorded in build-report.

## Remaining limits

No claim of all42tasks accepted: production scans/SBOM/license review, exact MySQL V6+ validation and historical migration rehearsal,50distinct-user performance, scanner failure infrastructure, real WeCom messages, WeChat devices, Linux/Nginx HTTPS deployment/privileges/alerts, immutable off-host audit and encrypted backup/binlog/PITR recovery remain separate gates. Paged API responses currently materialize authorized result sets in memory; large-data SQL pagination and EXPLAIN/performance evidence remain needed. The new typed schema is not a lossless automatic migration of an unseen legacy database.

No production credentials/data/services were changed, no PR merged, and no site was published. The user's2026-10-25 instruction is to finish this checkpoint and pause.
