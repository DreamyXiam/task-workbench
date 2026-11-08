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

Cross-company collaboration requests belonging to a DRAFT task are not exposed to the receiving company and cannot be accepted or rejected until the initiating leader confirms assignment. After confirmation, a pending request exposes only its existing minimal request fields; a formal collaboration subtask is created on acceptance. Requests on completed or cancelled tasks offer no actions.

Department heads and employees cannot view or act on draft task assignments before a leader confirms assignment. Authorized leadership retains draft access; after confirmation, the usual company, department and assignee checks apply.

`POST /notifications/archive` archives only read notifications currently visible to the authenticated user in the selected company and authorized task scope. Read notifications outside that scope remain available in their appropriate context.

`GET /notifications` accepts optional Boolean `read` alongside `page` and `pageSize`. It filters read/unread records after applying the authenticated user, company and task authorization scope, before calculating pagination totals. Omitting `read` returns both states.

`GET /tasks` and `GET /sub-tasks/mine` accept `page`, `pageSize`, `status`, `keyword`, `priority`, and `overdue`. Filtering is applied only after the actor's authorized scope is established and before pagination; keyword matches task title or code case-insensitively. `IN_PROGRESS` also includes `REWORK`, and task `PENDING_ACCEPTANCE` includes `PENDING_ACCEPTANCE_WITH_GAP`. `overdue=true` preserves historical lateness and compares submission or summary time to the deadline when available; otherwise it includes unfinished records past their deadline. The subtask list includes its parent task's title, code and priority while retaining the subtask status and deadline.

`GET /stats/dept` excludes draft tasks and applies task authorization before calculating department totals. Its overdue count uses the same historical, submission and summary time rules as task filtering.

KPI raw/formal input snapshots are immutable; heads receive only their authorized score data, never company-wide raw input snapshots. Ordinary admin infrastructure access does not grant score/task read access. Personal KPI remains disabled and candidate weights/calendars never change official30/25/20/10/15historical results.

New formal KPI runs and calendar simulations exclude sub-tasks of unconfirmed DRAFT tasks from their input snapshots and denominators. Confirmed tasks remain eligible under the existing period and cancellation rules; existing historical snapshots are not rewritten.

Remaining contract gates include complete typed request/error schemas, field-level negative cases, exact MySQL migration/concurrency, production scanner/quarantine, real-browser auth/download/WeChat, and the release acceptance matrix. Earlier13live-MySQL/35browser-fixture results are explicitly bound to their earlier build scope, not new routes.

## Role feature restrictions

`GET /api/v1/admin/role-permissions` returns `catalog` and the configurations for existing roles. A global administrator can `PUT /api/v1/admin/role-permissions/{role}` with `{ "version": -1, "disabled": ["TASK_VIEW"] }`; use the returned version for later changes. A missing configuration has version `-1`. Writes require a UUID idempotency key and recent TOTP; stale versions return `409`, unknown roles or permissions are rejected, and each change records before/after values and invalidates affected users' sessions. Restrictions on `ADMIN` apply only to company-scoped administrators: global administrators remain exempt and retain recovery access.

The supported switches cover task viewing, task creation, task handling, attachments, collaboration, reporting, KPI, organizational changes, notifications, user management, organization management, matching rules, notification channels, and audit logs. Disabling `TASK_VIEW` also disables task creation and handling, attachments, collaboration, reporting, notifications and organizational changes. User-management restrictions also apply to appointment administration; task classification requires both task-view and KPI access. The existing role, company, department, object, delegation, CSRF and state checks remain in force; switches cannot grant new access. Unclassified API endpoints are denied until assigned a permission or explicitly designated as shared infrastructure.