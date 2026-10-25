# ADR-003: Calendar and complexity policy remains simulation-only

Date: 2026-10-25. Status: implemented as a candidate analytical model; formal business policy pending.

The effective formal monthly assessment continues using its original natural elapsed-time metrics, five indicators, weights30/25/20/10/15, fixed snapshot history and7day objection period. No simulation endpoint can publish a new formal template, alter a formal score or cause a monthly job to adopt candidates.

The candidate model in `CalendarPolicy` offers a versioned company calendar (Asia/Shanghai, same-day work interval, weekly rest days, explicit holidays/makeup dates and scoped leave) and immutable task complexity/category versions. Missing classification defaults to NORMAL/NORMAL; ordinary coefficient is1. Candidate simple/complex coefficients must be within0.25–4; explicit candidate indicator weights sum to100. Weighted rates and complexity-normalized elapsed hours are a proposal for comparing different workload mixes, not an agreed personnel rule.

Leave treatment is explicit: subtract matching leave from elapsed working/natural hours and omit a sample whose deadline lies in matching leave. Intervals are half-open; overlapping exclusions count once; a user-specific interval applies only to that task's current snapshotted owner within the matching department. This is not a payroll, leave approval or attendance system. Overnight/rotating shifts and official-holiday synchronization require a later business decision.

Simulations run only for ended months, save their exact inputs/configuration/classification values and results, include SHA256 and baseline/candidate differences, and keep N/A visible for missing denominators. Department-level Pearson analysis highlights the possible overlap of timely/overdue indicators; low sample counts/zero variance produce N/A. Analysis does not choose new weights automatically.

Before any formal adoption, business owners must approve: whether workload affects denominators or thresholds, the coefficients, whether leave excludes entire tasks or elapsed intervals, applicability across department transfers, holiday data ownership, working/natural-time target conversion, sample minimums, and the effective future month. Approval must identify policy/template/calendar versions and preserve historical runs. Existing original weights cannot be overwritten by this ADR.

Implementation/test evidence: [calendar-report.md](../implementation/calendar-report.md). Final root acceptance includes exact-stack migration and UI checks; local H2 results alone do not establish production readiness.
