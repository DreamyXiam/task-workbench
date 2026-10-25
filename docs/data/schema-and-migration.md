# Schema and migration conventions

This project starts from an empty schema. `business_record` stores aggregate kind, organization/task/owner scope, payload, created/updated timestamps and optimistic version. Typed relational entities hold authoritative core fields and foreign keys. `Records` writes both in the same transaction; reads overlay typed columns. Dynamic projection names/columns come only from constant whitelists, never caller SQL identifiers.

| Area | Authoritative entities / invariant |
|---|---|
| Organization/auth |company, department, app_user, user_scope; enabled scopes are selected individually, never unioned; active company/department relations checked server-side.|
| Tasks |task, sub_task, sub_task_assignee; host-company composite FK, one generated unique host per task, unique(task,department), current and original deadlines, per-subtask overdue/reject count.|
| Process history |task_comment, task_progress, deadline_request, task_deadline_history, collaboration_request, result_version; immutable prior submissions/deadline changes and consent before foreign execution.|
| Matching |function_tag, matching_rule, matching_feedback plus immutable MATCHING_RESULT aggregate snapshots; nullable typed company means global, with authorization restricting configuration.|
| Notifications/jobs |notification_outbox, notification, notification_delivery, job_execution; recipient/event/channel uniqueness, retry/lease identities and transaction-bound business effects.|
| KPI |kpi_template, kpi_run, kpi_score, kpi_appeal, kpi_adjustment, monthly_run_key; unique official period/template run and department score, immutable raw/input snapshot, separate adjustment facts.|
| Files/audit |attachment, audit_event, audit_chain_anchor; nonpublic random storage keys, quarantine state, scoped reads, serial hash-chain append.|

Canonical names: `sub_task.first_received_at` is the first department receipt (not final acceptance); `submitted_at` is the current submission; main `task.accepted_at` is final acceptance. Creation/version fields are inherited from the aggregate envelope and returned in the merged API view. Notification uses its actual `read_at`/`sent_at`/`archived` columns, not fictitious `is_read` aliases. Historical legacy `received_at`/`accepted_at` must be mapped by meaning before any import; this project does not infer a migration from ambiguous names.

Append-only migrations: V1foundation; V2typed entities; V3TOTP counters/audit anchor; V4atomic company daily number; V5scope invariants/deadline/overdue facts; V6durable WeCom delivery; V7global matching scope; subsequent checkpoint migrations extend appointments/delegation/handover and simulation only. Never alter an applied migration or run an automatic destructive down migration. Restore an approved consistent backup if a reviewed rollout requires rollback.

H2 is a synthetic demo/test adapter, not proof of MySQL syntax/locks/index use. Earlier MySQL8.4.13 V1-V5 migration passed. Final V6+ and historical data rehearsal, constraints, actual EXPLAIN and least-privilege denial tests remain required before release.
