# CDEX-022 / CDEX-023 matching implementation

Scope: deterministic department suggestions, immutable matching explanations, and explicit human feedback. This report does not claim completion of all V1.5 acceptance items.

## Source changes

- `backend/src/main/java/cn/workbench/MatchingEngine.java`: pure Java matching engine.
- `backend/src/main/java/cn/workbench/MatchingController.java`: authorized history, feedback, and adoption statistics.
- `backend/src/main/java/cn/workbench/OrganizationController.java`: replaced only `/tasks/match`; added the authorized `scope=GLOBAL` TAG/RULE creation branch requested by the integration owner. Also added the explicitly requested readonly collaboration-department options endpoint. Other administrator and role policies are unchanged.
- `backend/src/test/java/cn/workbench/MatchingEngineTest.java`: engine tests and database integration regressions.

Global relational support is provided separately by the integration owner's V7 migration and Records projection. Global TAG/RULE storage uses business-record company0 and nullable typed company_id; it is available only to an explicitly global ADMIN. Candidate departments always come from the requested company. Global rules do not infer a new department from its name or code.

## Matching behavior

Input normalization uses Unicode NFKC, Locale.ROOT lowercasing, format-character removal and whitespace normalization. ASCII keywords require word boundaries; Chinese keywords use literal phrase matching. Configured negative keywords exclude their source rule. A literal Chinese/English negation prefix excludes that occurrence; a later affirmative occurrence can still match. Each normalized keyword is counted once per source, even if input repeats it.

Company-specific configuration overrides global configuration with the same kind and logical code/name. A disabled company override suppresses the corresponding global configuration. Within the winning scope, identical terms may legitimately suggest several departments. The latest explicit rule version and source ID resolve duplicate revisions for one department.

Candidate ordering is: exact normalized title/content fixed phrase; contained fixed phrase; ordinary keyword. Fixed phrase candidates then prefer the longest phrase, explicit numeric priority, accumulated configured weight, and finally department ID. Equal business ranks are explicitly marked as host candidates and require a human to select one host. A collaborator-only fixed rule cannot be promoted to host by a weaker ordinary tag. Roles and specificity are taken from the strongest matching rules. No result changes assignments automatically. Zero matches retain a manual selection path.

Each hit includes source kind/ID, matched term, configured weight, match type, outcome, normalized offset and source rule version. Excluded/negated hits remain explainable. An unambiguous canonical configuration encoding produces the version fingerprint. Persisted results contain the original input, normalized input, input hash, complete effective configuration snapshot, suggestions and explanations; subsequent rule changes do not rewrite them.

## API additions

`POST /api/v1/tasks/match` keeps the fixed contract (`suggestions`, `ruleVersion`) and adds `matchingResultId`, `engineVersion`, `manualFallback`, `tiedHostDeptIds`, `excludedHits` and `inputHash`. Each suggestion still exposes `deptId`, `deptName`, `role`, `score`, `reasons`, plus structured `hits` and confirmation flags. The request remains company-scoped and leader-only. Spring Security enforces CSRF at HTTP entry; `Writes` enforces UUID idempotency for this mutation.

`GET /api/v1/matching/results/{id}` returns immutable history only to the owner in an authorized leader scope or a GROUP_LEADER. It does not return the original input body.

`POST /api/v1/matching/feedback` accepts `{taskId, matchingResultId, reason}`. The server checks task scope/creator authority, matching owner and company, and the task input hash. It derives `ACCEPTED` or `OVERRIDDEN` from the persisted suggestion and actual task department assignments; a client-supplied decision cannot falsify this outcome. Identical task/result feedback is immutable and deduplicated under a task lock, even with a new idempotency key. Feedback is audited and never publishes or modifies a rule.

`GET /api/v1/matching/feedback?companyId=...&page=1&pageSize=20&from=...&to=...` returns authorized feedback and sample count, accepted/overridden counts, and suggestion adoption rate. The statistic is explicitly not matching accuracy. A group leader must select one company. Optional From is inclusive and To is exclusive; bounds accept ISO8601 timestamps or YYYY-MM-DD dates interpreted at midnight Asia/Shanghai. Invalid or reversed bounds are rejected. A zero sample has a null rate.

`GET /api/v1/organization/collaboration-departments` is leader-only and returns only `{id,name,companyId,companyName}` for enabled departments in enabled companies. This expressly permits cross-company collaboration option selection without exposing users, tasks or other business data. The existing `/organization/departments` scope is unchanged.

## Verification

- RED: a minimal empty engine ran 12 JUnit test methods via an isolated reflection runner: 10 failed, 2 passed. Failures covered missing normalization, matching, negatives, overrides, explanation and fallback behavior.
- GREEN: the first12 tests passed after implementation.
- RED: two additional ambiguity/English-negation regressions failed (same-name sources incorrectly collapsed departments; `knot tax` incorrectly treated as `not tax`).
- GREEN: all14 pure engine tests passed on Amazon Corretto21.0.13 with isolated `javac` and the same cached JUnit/AssertJ assertions. No Maven target directory was modified during another worker's Maven window.
- RED: the time-window regression initially found one sample instead of zero for a future From bound; the fixed-collaborator regression initially found HOST instead of COLLABORATOR when an ordinary tag matched the same department.
- GREEN: final focused Maven run completed with exit0, **21 tests / zero failures / zero errors**: 15 pure engine tests and6 Spring/database/HTTP cases. It used Spring Boot3.5.16, Flyway11.20.3, Corretto21.0.13 and fresh H2 migrationsV1–V7. Cases include role/scope and input mismatch rejection, immutable historical explanations, server-derived override feedback and deduplication, globalADMIN-only rule creation, time bounds, and HTTP employee403/minimal leader options/disabled-company filtering.

Reproducible focused command, from `backend/`:

```powershell
$env:JAVA_HOME='D:\TASK\task-workbench\.cache\corretto21\jdk21.0.13_0'
& E:\DevEnv\Maven\apache-maven-3.9.12\bin\mvn.cmd -o -s ../tooling/compatibility/cache-settings.xml '-Dmaven.repo.local=../.cache/m2' '-Dtest=MatchingEngineTest' test -q
```

Surefire evidence: `backend/target/surefire-reports/TEST-cn.workbench.MatchingEngineTest.xml` and `TEST-cn.workbench.MatchingEngineTest$FeedbackApi.xml`. The parent integration owner will run the complete suite and package once all workers release their Maven windows.

## Limits

Matching uses configured literal rules and bounded heuristics, not semantic NLP or external AI. Ambiguous/negated language outside the configured dictionary still needs human review. The additional feedback/history endpoints are implemented; automatic UI wiring of matching feedback is outside this change's frontend ownership. Accepted feedback measures agreement with the suggested host and first zero-to-two collaborator suggestions, never independently labeled accuracy. MySQL migration and end-to-end smoke evidence is owned by the integration report; the engine's isolated JDK checks do not imply MySQL acceptance.
