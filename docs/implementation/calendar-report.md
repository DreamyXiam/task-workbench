# CDEX-027 calendar and complexity simulation

Date: 2026-10-25 (Asia/Shanghai). Scope: candidate configuration and deterministic simulations only. The formal `Kpi`, `KpiController` and monthly `Jobs` code is unchanged. This report is local implementation evidence, not production or business-policy sign-off.

## Implemented

- V9 adds four independent tables: `company_calendar_version`, `kpi_simulation_template`, `task_classification_version`, `calendar_simulation`. Configuration/classification versions append; no update/delete/publication API exists. Company/task/department foreign keys and per-company or per-task version uniqueness are enforced.
- Company calendars explicitly use Asia/Shanghai, natural or working hours, weekly rest days, holidays, makeup work dates and department/optional employee leave intervals. Overlapping leave is subtracted once. A candidate excludes a sample when its deadline falls in applicable leave. This exclusion and hour normalization apply only to that simulation.
- Task classification defaults to NORMAL complexity and NORMAL category. Leaders append reasoned versions with optimistic version checks. Changing classification does not overwrite past formal scores or stored simulations.
- Five candidate weights must sum to100; ordinary complexity stays1, simple/complex coefficients range0.25–4. The candidate weights rates by complexity and normalizes response/execution hours by that coefficient. This is a proposed analytical model, not approved formal policy.
- Simulations use ended months and a bounded maximum2000 subtask samples. Cancelled tasks are excluded. Zero applicable denominators remain N/A; a zero-weight N/A indicator is omitted rather than silently becoming100. Baseline and candidate are evaluated from the same saved sample set; output includes metrics, indicator scores, final totals and differences.
- Frozen inputs include calendar values/ID, template values/ID, classification versions, minimal calculation facts, timezone and policyVersion1, with SHA256. Historical reads return the stored result and validate the input hash; later task changes cannot recalculate it silently. Raw task text/results and input snapshots are not returned in policy/result APIs.
- Pearson analysis uses departmental timely/overdue rates, requires at least3 samples and nonzero variance, and explicitly reports insufficient evidence otherwise. It never updates weights.

## Authorization

ADMIN can configure only its infrastructure company scope and requires fresh TOTP. It cannot run/read business simulations. Leaders can simulate authorized companies. Department heads can simulate/read only their own department, cannot retrieve a company-wide saved result, and see only their department's leave entries in calendar listings. Employee access is denied. Cross-company calendar/template IDs, foreign department leaves and unauthorized employee leave ownership are rejected. All writes require existing Session/CSRF/idempotency middleware and append audit events; calendar/classification version conflicts return409.

## API

- `GET /api/v1/kpi/calendar-policy?companyId=...` returns calendars/templates/saved simulation metadata and allowedActions.
- `POST /api/v1/kpi/calendar-versions` accepts companyId/name/reason/expectedVersion/calendar; creates one immutable candidate.
- `POST /api/v1/kpi/simulation-templates` accepts companyId/name/reason/weights/complexityWeights and requires simulation=true.
- `POST /api/v1/kpi/calendar-simulations` accepts companyId/period/calendarVersionId/templateId/reason/simulation=true and optional deptId. ID0 selects the original natural-time/original-weight baseline.
- `GET /api/v1/kpi/calendar-simulations/{id}` returns authorized frozen rows and correlation analysis.
- `GET|POST /api/v1/tasks/{id}/classification` reads default/latest classification or appends a reasoned optimistic version.

The existing `CalendarSimulation.vue` request/response contract was retained and communicated to the frontend owner. That page's integration/build is verified by its owner/root, not inferred from Java tests.

## Verification

Focused command (repository root):

```powershell
$env:JAVA_HOME='D:\TASK\task-workbench\.cache\corretto21\jdk21.0.13_0'
& E:\DevEnv\Maven\apache-maven-3.9.12\bin\mvn.cmd -o -s tooling/compatibility/cache-settings.xml '-Dmaven.repo.local=D:\TASK\task-workbench\.cache\m2' -f backend/pom.xml -B -ntp '-Dtest=CalendarPolicyTest,CalendarSimulationTest' test
```

Final focused result: **12 tests passed,0 failures,0 errors,0 skipped; exit0** on2026-10-25 at11:12 Asia/Shanghai. Runtime: Corretto21.0.13, Boot3.5.16, Flyway11.20.3, H2 MySQL mode; all V1–V9 migrations applied successfully. Evidence: `.cache/reports/calendar-final.log`. First implementation run executed12 tests:9passed and3errors due H2 returning generated id plus created_at; corrected JDBC insertion to request only the id column. A subsequent attempt was blocked by an organization controller edit; the final rerun above verifies the stable combined source. The original absent-storage assertion and missing-class compilation established missing implementation, not completed behavior verification.

Coverage includes cross-month holidays/makeup days, overlapping leave, unrelated department leave, zero denominators, explicit deadline exclusion, unchanged formal weights, independent tables, version conflict, scoped admin, foreign department rejection, department-bound simulations, no formal run writes, immutable historical results, candidate-only templates and classification conflicts.

## Migration, rollback and limits

Append V9 after existing migrations; never edit deployed checksums. It adds candidate-only tables and does not transform old formal KPI rows. Rollback the application only after compatibility review; retain these tables and frozen results, do not drop them automatically. Real MySQL8.4 migration, combined HTTP/UI tests and load evidence belong to root's integration report. No production credentials, scanner, external AI, real WeCom messages or business data were used here.

Formal calendar adoption, leave exclusion policy, complexity factors, working-day thresholds and altered weights remain business approval decisions in ADR-003. No legal-holiday service is contacted; operators supply versioned dates. Employee leave intervals are bounded configuration rather than an HR/attendance product. The class uses one same-day work interval; shifts spanning midnight and individual rotating shifts are outside this candidate policy. Employee-specific leave configuration currently requires an active matching company/department membership; importing leave for inactive historical members needs a later explicit import rule. No original signed release acceptance or real-device validation is claimed.
