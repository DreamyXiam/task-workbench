# CDEX-009 organization changes and delegation evidence

Date: 2026-10-25, Asia/Shanghai. This report covers the current implementation increment and necessary CDEX-007 authorization fixes. It does not approve production release or claim every CDEX-009 operational acceptance case is complete.

## Implemented behavior

Appointments now have `valid_from`, `valid_until`, `employment_status` (`ACTIVE`/`ON_LEAVE`/`LEFT`), `appointment_type` (`PRIMARY`/`CONCURRENT`) and an optimistic `version`. Existing rows default to active, primary, unlimited appointments. Authorization/login/member selection uses only effective appointments and enabled account/company/department. Historical and expired appointments remain available to authorized administration; they are not deleted. An appointment change requires administrator scope, fresh TOTP, version and reason, appends an immutable change snapshot, and increments the account session version. A company administrator cannot alter a shared account with any foreign/global historical scope, including an expired foreign appointment.

Delegation is an explicit session context. A real department head can grant a different active member of the same company and department an explicit subset of task operations, with start/end/reason. Grants last at most 31 days. This is a least-privilege implementation limit; delegation of administrator or leadership powers is not implemented. Creating, activating and revoking requires fresh TOTP. Delegated users cannot re-delegate or combine grants. Every request checks the current grant, delegator appointment, both accounts, current recipient appointment, organization, window and revocation. Task reads remain within the delegated department; KPI, administration, reports and unrelated mutation endpoints are denied. Ordinary contexts remain separate. Leaving the delegated context works even after its grant expires or is revoked.

Handover changes only current in-progress responsibility. Initial assignment preserves `originalOwnerSnapshot`; later transfer records prior/new responsibility snapshots, actor, optional delegation ID and reason in typed `task_handover` and the timeline. Existing task-detail `transfer` and replacement-owner `assign` paths use the same implementation. Replacement assignment requires the current version, reason and, for delegated actors, explicit transfer permission. Original snapshots, prior result versions and KPI history are not rewritten. A batch of 1–100 transfers runs in one transaction and validates company/department membership, task/subtask state and each version.

Audit events retain the actual person's ID and nullable `delegation_id`; delegated timeline events carry grant identity. Ordinary audit-hash material is unchanged; delegated events append `|delegation=<id>`. Audit verifiers must account for that additional field for delegated events. Idempotency fingerprints now include the active delegation ID as well as actor/scope/request. Reusing a pre-change key with a different fingerprint returns 409 rather than executing under a new authorization context.

## API contract

All paths below start with `/api/v1/organization`; normal Session/CSRF and standard API envelopes apply. Business writes using `Writes.run` require a UUID `Idempotency-Key`.

| Endpoint | Scope and body/result |
|---|---|
| `GET /appointments?userId=` | ADMIN; managed appointment `items`, versions and `allowedActions.edit`; user filter optional |
| `PUT /appointments/{id}` | ADMIN + fresh TOTP; `version,employmentStatus,appointmentType,validFrom,validUntil,enabled,reason`; role/company/department identity is not reassigned through this endpoint |
| `GET /delegations` | Own grants in current company/department; `items`, effective flag, permissions and per-row `activate/revoke`; top-level `allowedActions.create` for a real department head |
| `POST /delegations` | Real DEPT_HEAD + fresh TOTP; `delegateUserId,permissions,validFrom,validUntil,reason`; permission names: receive, assign, progress, submit, withdraw, extend, transfer, summarize, comment, upload |
| `POST /delegations/{id}/activate` | Named recipient + fresh TOTP; explicit session context, no role union |
| `POST /delegations/{id}/revoke` | Original delegator + fresh TOTP; `version,reason` |
| `POST /delegations/leave` | Restore ordinary session context; no business-data mutation |
| `GET /handover-tasks` | Ordinary head/leader; manageable active subtask `items` with current owner, task title, version and `allowedActions.transfer` |
| `POST /handovers` | Ordinary head/leader + fresh TOTP; `{items:[{subTaskId,userId,version}],reason}` |
| `GET /handovers` | Ordinary head/leader; scoped append-only history, newest 1,000 rows |

`GET /api/v1/auth/me` additionally returns optional `delegation:{id,delegatorId,companyId,deptId,permissions,validUntil}` and effective department-head role while active; `scopes` remain the person's own appointments. Auth context switching exits delegation. Expected denials are 403, stale state/version is 409, malformed policy/time/body is 422, and missing idempotency key is 400.

## Files changed

- `backend/src/main/resources/db/migration/V8__appointments_delegation_and_handover.sql` (new additive migration only).
- New `OrganizationChanges.java`, `OrganizationChangesController.java`, `OrganizationChangesTest.java`.
- Necessary integration: `Store.java`, `Policy.java`, `Auth.java`, `Writes.java`, `TaskService.java`, and the users-only admin paths of `OrganizationController.java`.
- Frontend integration is owned by the frontend increment and recorded in its report.

## Actual verification

Runtime: Corretto Java 21.0.13, Spring Boot 3.5.16, MyBatis Starter 3.0.5, Flyway 11.20.3. Synthetic-only test profile and fresh H2 MySQL-mode database. No real account, message or production schema was used.

The initial build was blocked by another independent test's not-yet-written class; that is not counted as behavioral red evidence. The rerun in `.cache/reports/organization-behavior-red.log` executed 2 tests and failed 1 assertion: expired `Store.scope` still returned an active scope. After implementation, the first 10 focused tests passed. Final command:

```powershell
$env:JAVA_HOME='D:\TASK\task-workbench\.cache\corretto21\jdk21.0.13_0'
& E:\DevEnv\Maven\apache-maven-3.9.12\bin\mvn.cmd -o -s tooling/compatibility/cache-settings.xml '-Dmaven.repo.local=D:\TASK\task-workbench\.cache\m2' -f backend/pom.xml -B -ntp '-Dtest=OrganizationChangesTest' test
```

Final result at 2026-10-25 11:14:45 +08:00: **exit 0; 14 tests, 0 failures, 0 errors, 0 skipped**. Evidence: `.cache/reports/organization-final.log`. Fresh H2 migrations V1–V9 applied successfully.

Covered cases: compatible appointment defaults; expired/on-leave/future/disabled-organization rejection; same-department and permission validation; explicit activation without authority union; unrelated KPI rejection; immediate revocation; expired/unavailable-delegator rejection; original owner preservation and append-only transfer history; foreign-department target denial without changes; TOTP gate and session-version revocation on departure; delegated-context idempotency mismatch; actual-person/grant audit provenance; shared expired-foreign-scope appointment denial; delegated assignment cannot bypass transfer permission; expired-foreign-scope account password-reset/disable denial.

## Remaining acceptance and migration limits

- Final whole-project regression/package is the integration owner's next verification; 14 focused tests are not that result.
- V8 has been run on fresh H2, not newly verified against MySQL 8.4 in this increment. Real MySQL migration, concurrent multi-task batch rollback and browser/backend auth flows remain separate acceptance work.
- Leadership/administrator delegation, arbitrary role-permission configuration, HR synchronization and retroactive business ownership changes are intentionally outside this least-privilege implementation. Business expansion needs explicit design/approval.
- No destructive rollback is provided. Revert application code only after schema compatibility proof; otherwise restore a verified backup into an isolated replacement schema. Do not delete historical handover/appointment/audit rows or edit released migration checksums.
- Existing V1–V7 migrations and historical task/KPI results were not rewritten. Production deployment, real-data migration and user acceptance were not performed.

The requested current part is complete at this checkpoint; no follow-on task is started by this report.
