# Health, metrics and alerts

The deployment places management on loopback8081, and Nginx denies /actuator. Readiness is for an internal probe, with no business details exposed. Health checks never bypass authorization for business APIs. Backend metric names and the final actuator security matcher must be verified against actual implementation; the following alerts are operational requirements, not already configured monitoring infrastructure.

| Signal | Initial operational trigger (tune in staging) | Response |
|---|---|---|
| Readiness/app process | failed twice over60s | page operator; inspect DB/disk; avoid restart loops |
| API p95/error rate | p95>1s or5xx>1% for5min | inspect request trace IDs, pool saturation, slow queries |
| CPU/thread/DB connection pool | sustained saturation or rejected requests | preserve diagnostics, stop nonessential jobs, investigate blocked transaction |
| Free disk | warning<20%, critical<10% | page; verify backup/log growth; never delete KPI/object history ad hoc |
| Notification outbox | oldest pending>5min or terminal failure | inspect channel/network; retry with same identity; verify no duplicate business effects |
| Scheduled jobs | missing expected completion or repeated failed execution | inspect execution key/timezone; replay explicitly; compare monthly run uniqueness |
| Backup/binlog/object archive | no successful batch in approved window, archive gap, checksum mismatch | page; stop claiming RPO; repair archive and run restore drill |
| Security/audit shipping | denial spike, TOTP replay, failed append/off-host ship | preserve evidence, contain scope, rotate compromised material |

Instrument both endpoint latency and queue lag; do not label metrics with user names, titles, tokens or unbounded resource IDs. Store trace IDs in structured logs without credential/request-body data. Dashboard access is restricted to operations, with audited temporary emergency access under ADR-007.

Failure drills on isolated staging only: stop DB, fill a quota-controlled disposable volume, fail channel delivery, kill worker mid-job, restart at month boundary, interrupt backup and log shipping. Record alert firing, recovery, missed/duplicate delivery and immutable KPI effects. Do not fill or interrupt a real production disk/database. Current status: these injected drills and performance targets are pending.
