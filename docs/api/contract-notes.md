# API contract checkpoint

Updated2026-10-25. `openapi.yaml` is OpenAPI3.0.3 in JSON-compatible YAML. Existing source inventory is supplemented with matching, minimal collaboration directory, scoped appointments/delegation/handover, calendar/classification/simulation and actual XLSX/PDF exports. It is a source-derived draft, not proof every operation passed HTTP conformance tests. Some request schemas remain open maps and require tighter DTO/field-level documentation before release. Managed Actuator is separate.

Authentication uses same-origin TASK_SESSION with HttpOnly/Secure/SameSite in production, CSRF for unsafe methods and UUID Idempotency-Key for business writes. Success is `code/message/data/traceId`. Negative responses distinguish401session,403authorization/fresh-TOTP,409state/version/key mismatch,422validation,429throttle and503unavailable dependencies. Clients reload after409 and never infer authority solely from a hidden button. Every resource operation rechecks server scope/relation/state.

New checkpoint routes:

- `GET /organization/collaboration-departments`: enabled minimal id/name/companyId/companyName options, leadership only; does not authorize foreign execution.
- `/matching/results/{id}` and `/matching/feedback`: immutable explanation snapshot and feedback derived from persisted assignment, scoped adoption counts. See matching-report.
- `/organization/appointments`, `/organization/delegations`, `/organization/handover-tasks`, `/organization/handovers`: appointment windows, explicit delegated context, reasoned versioned batch handover/history. See organization-changes-report for methods/DTOs and restrictions.
- `/kpi/calendar-policy`, `/kpi/calendar-versions`, `/kpi/simulation-templates`, `/kpi/calendar-simulations`, `/tasks/{id}/classification`: append-only candidates and reproducible simulation, never silent activation in official assessment. See calendar-report.
- `/stats/export` and `/kpi/export`: format=xlsx(default)/pdf/csv; fresh TOTP, authorized same-snapshot scores, actor/time watermark, text-only formula-like XLSX cells,5000row/200page bounds, per-user2second and2concurrent rate limits. PDF uses operator-configured trusted Chinese TTF and returns503if unavailable.

Task code is RW-companyCode-YYYYMMDD-sequence. Main lifecycle uses DRAFT, IN_PROGRESS, PENDING_ACCEPTANCE, PENDING_ACCEPTANCE_WITH_GAP, REWORK, COMPLETED, CANCELLED; overdue is an independent historical flag. Subtask states and actual allowedActions come from TaskService. Withdrawal before final acceptance invalidates host summary; rework requires fresh result versions and host summary before acceptance. Gap closure does not fabricate acceptance for missing departments.

`POST /notifications/archive` archives only read notifications currently visible to the authenticated user in the selected company and authorized task scope. Read notifications outside that scope remain available in their appropriate context.

KPI raw/formal input snapshots are immutable; heads receive only their authorized score data, never company-wide raw input snapshots. Ordinary admin infrastructure access does not grant score/task read access. Personal KPI remains disabled and candidate weights/calendars never change official30/25/20/10/15historical results.

Remaining contract gates include complete typed request/error schemas, field-level negative cases, exact MySQL migration/concurrency, production scanner/quarantine, real-browser auth/download/WeChat, and the release acceptance matrix. Earlier13live-MySQL/35browser-fixture results are explicitly bound to their earlier build scope, not new routes.
