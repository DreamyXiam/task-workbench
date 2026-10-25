# CDEX-024: durable station and enterprise WeCom delivery

Status: implemented, focused12case verification passed; no real external message or configured Webhook has been used. All delivery tests use synthetic in-memory H2 fixtures and an injected fake Sender/secret resolver. Initial negative endpoint tests reject before constructing any network client.

## Source changes

- New `WecomDelivery.java`: transactional outbox materialization, independent station/WeCom delivery rows, claim leases and fencing tokens, bounded retries, company-specific external key lookup, injectable external transport and real production Java HttpClient boundary.
- `Jobs.outbox()` delegates to this worker; reminder/monthly and other Jobs methods were not changed.
- Only delivery-related Store mapper methods were appended. Existing notification read/scope fields and business outbox writes are unchanged.
- Append-only `V6__wecom_delivery.sql`: per-outbox/type/channel unique delivery key, status, attempts, next retry, lease token/deadline, stable last-error code and completion timestamp. No credentials or Webhook URLs are stored.
- `WecomDeliveryTest.java`: real repositories/transactions and synthetic fake boundary; no live external sending.

## Transaction and failure behavior

Business writes retain their existing transactional outbox insertion. A worker invoked inside any active business transaction returns without delivering; uncommitted/rolled-back events cannot escape. Committed PENDING outbox rows materialize station plus currently enabled ACTIVE WECOM channels in the **same company** and become DELIVERING. Station insert is deduplicated by existing unique outbox_id and commits with its delivery completion.

Each delivery is claimed under a database lock, with a committed60second lease/token before external work. External transport runs without a database transaction/row lock. Completion/failure is fenced by the current token. Expired leases can be reclaimed after restart. First failed attempt retries after2seconds, exponentially backing off to at most3600seconds. After8claims/failures delivery becomes DEAD; the station notification is not repeated. Disabled pending channels become SKIPPED. Outbox status aggregates child state; durable child rows retain attempt/due/error details. Errors store controlled codes only, never exception text, URL, response body or key.

WeCom is **at least once**: a crash or timeout after vendor acceptance but before database acknowledgment can cause a repeated group message. No distributed exactly-once claim is made. Dedupe is per outbox ID and channel; existing business outbox rows are per recipient with UUID event keys, so a company group may receive separate messages for multiple recipient events. Coalescing distinct recipient events is not silently inferred. Newly enabled channels do not retrospectively replay already materialized events. A terminal requeue requires an approved operator procedure; no automatic destructive reset is supplied.

## Operator configuration (not executed)

Create a company CHANNEL with type=`WECOM`, `envKey=TASK_WECOM_COMPANY_<companyId>_KEY`. Creation remains disabled by default in OrganizationController. An operator supplies only the opaque Webhook key in that externally managed environment variable; the value must never be submitted to the ordinary channel API. An authorized administrator with fresh TOTP may enable the channel through the existing versioned update API after company routing is reviewed. The worker requires an exact company-bound envKey name; arbitrary environment variable lookup and cross-company key references are refused. Channel type/envKey are specified on creation; use controlled recreation if that immutable configuration needs changing.

The actual URI is constructed internally as `https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=<external-key>`. Transport revalidates HTTPS, exact host/raw path,443/default port, no user info/fragment and only the expected key query; redirects are disabled. Connection timeout3seconds; an8second future deadline bounds the complete request/response and cancels overdue work; a BodySubscriber caps responses at64KiB. Only HTTP200 with numeric vendor errcode exactly0 counts as success. Notification text contains an internal event/task reference and the scoped outbox title; keys never enter message bodies, ordinary API responses or delivery rows.

Monitor notification_delivery for DEAD rows, stale IN_FLIGHT leases, pending age and attempt spikes; do not infer all-channel success from the station record alone. Real tenant routing, upstream availability/rate limits and approved end-to-end WeCom delivery remain deployment acceptance work. No real key was configured or actioned here.

## Actual verification

Candidate stack: Boot3.5.16, MyBatis3.0.5, Java21.0.13, Flyway11.20.3, synthetic H2/MySQL mode. Maven used exact candidate dependencies cached by the integration owner; `-o` was dependency-cache mode, **not** the legacy `offline-verification` profile.

Command from project root:

```powershell
mvn -o -f backend/pom.xml -s tooling/compatibility/cache-settings.xml -Dmaven.repo.local=D:\TASK\task-workbench\.cache\m2 -Dtest=WecomDeliveryTest test
```

Observed TDD sequence: initial station durability test failed with1assertion failure/0errors because the delivery table was absent; station increment then passed1/0/0. The first7case fake-boundary run failed4expected delivery/attempt assertions against the station-only implementation. The full worker then exposed an eager HttpClient selector initialization denied by the sandbox and a timestamp-precision issue; client initialization was made lazy and due timestamps normalized to milliseconds. Subsequent7case run passed7/0/0 with V1–V7 migrations applied to synthetic H2. Final12case run passed12tests/0failures/0errors/0skipped, Maven exit0 at2026-10-24T22:36:16+08:00 (12.223seconds). Cases cover durable station dedupe, same-company external routing and after-commit send, retry/restart without station duplication, expired leases/stale-ack fencing, business rollback/active-transaction refusal, disabled channels, foreign secret-reference refusal, terminal8attempt cap, disabling a pending channel, malicious endpoint rejection, explicit vendor acknowledgment and bounded response cancellation. No real network client was initialized for these fake or rejection paths. Root owns full-suite/MySQL8.4 integration verification after all workers finish; neither real WeCom transmission nor production routing is claimed verified.
