# Current code review record

Date2026-10-25. Scope: source review of authorization/state/history/export/matching/outbox and the current appointments/delegation/calendar increment, backed by101executed tests. This is not a completed independent penetration test or production sign-off.

| Finding | Correction and evidence |
|---|---|
| Malformed company-scoped GROUP_LEADER could act globally | Domain rejects it; scope creation forbids it; migration check and negative tests. |
| Scoped admin could alter a shared/global account | All raw historical scopes checked, including expired foreign appointments; organization regression denies reset/disable. |
| Unassigned employee/task metadata leak | Object authorization requires assignment/ownership; outbox recipients restricted; regression checks zero employee notifications. |
| Department head could receive company-wide KPI inputs | Public run serialization strips inputSnapshot; department score data remains scoped; dedicated negative test. |
| Task-level attachment could bypass department scope | Shared Policy attachment check used by detail/version/download paths; department-negative regression. |
| Withdrawal/rework left obsolete host summary or fabricated gap acceptance | Pre-final withdrawal clears summary freshness; targeted rework preserves prior versions and requires new summary; missing gap departments not marked accepted. |
| Parent lateness and rework denominator distorted departmental KPI | Independent subtask overdue facts; accepted-only rework denominator; zero denominators N/A; golden and regression cases. |
| Concurrent audit append could fork the chain | Transaction locks durable anchor;30parallel events verify one chain. |
| Missed monthly cron did not settle after restart | Regular scheduler catches the prior month using durable unique execution/run keys; repeated catch-up regression. |
| ZIP/legacy Office format policy lacked required validation | Magic/OLE signature checks, path/size/entry/expansion bounds, fail-closed scan; attachment safety tests. |
| Notification retries lacked per-channel persisted identity | Station and company-key WeCom records, lease token/fencing, retries/terminal status;12focused tests. |
| Reassign and legacy transfer bypassed responsibility history | Both invoke shared handover implementation; first owner snapshot, immutable prior/new/actor/reason facts and version checks;14organization tests. |
| Delegation context could reuse a prior authorization fingerprint | Explicit grant context with per-request effectiveness/path permission checks; idempotency includes delegation ID; actual actor/grant audited. |
| Export was CSV-only and lacked fresh-TOTP/throttle coverage | Actual XLSX inline-text/PDF same authorized snapshot, watermarks/size/rate limits;6export tests plus independent XLSX/PDF parsing and PDF render inspection. |
| Persistent H2 demo lost its generated TOTP key on a new terminal | Demo-only Windows DPAPI key file retained beside isolated DB; conflicting key refused. Script parser passed; launcher network smoke remains blocked. |

Current unresolved scope is listed in build-report and acceptance-matrix: final MySQL/HTTP/browser/device, production scan/SBOM/license review, large data performance/SQL pagination/EXPLAIN, HTTPS/alerts/audit retention and real recovery drills. No claim that a successful compile or synthetic fixture closes those gates.
