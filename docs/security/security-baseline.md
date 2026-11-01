# Security baseline and evidence gates

Online frontend and source security jobs passed on 2026-11-01; see the open-source readiness evidence for exact commits, tools and remaining checks. This does not establish production acceptance. Release blocks on unresolved exploitable HIGH/CRITICAL vulnerabilities; a scanner outage or stale advisory feed is a failed gate, not a clean scan. Any exception needs owner, exploitability evidence, compensating control and expiry approved before release.

| Control | Implementation / required negative proof |
|---|---|
| Session and CSRF | Session rotation on login, HttpOnly/Secure/SameSite cookie, 8h idle timeout; missing/incorrect CSRF denies writes; logout/password/role/disable invalidates old sessions |
| Company/object isolation | Backend scope checks on reads, files, exports, statistics, background jobs; guessed company-B IDs must never reveal body or metadata to company-A |
| Privileged actions | TOTP for leaders/admins, encrypted external-key secrets, hashed single-use recovery, replay protection/rate limits and step-up for export/unlock/adjustment |
| Credentials | No secrets in Git, logs, CI output or UI state; environment/option files root0600; separate runtime, migration, backup and restore identities; documented rotation |
| Files | 10MB allowlist/type/magic/path checks, random immutable object keys outside web root; quarantine and scanner failure deny download; no untrusted active preview |
| Output | Encode XSS; SQL bind values and allowlist sort; Excel formula-leading characters forced to text with exporter/time watermark and audit |
| Audit | Business/security logs separate from request logs; actor/scope/trace/reason retained; append-only off-host storage with integrity verification; failures alert |
| Edge | HTTPS same origin, fixed trusted forwarded headers, reject unknown Host, private loopback backend/management, CSP and nosniff; TLS/browser checks pending |
| Supply chain | Lock npm, explicit candidate BOM, clean build, license review, SBOM, independent secret/SAST/dependency gates |

Required CI tools: Gitleaks8.28.0 redacted all-ref history detection exits1 on a finding; Semgrep1.139.0 OWASP registry rules fail on findings; Trivy0.67.2 fails on HIGH/CRITICAL findings in the source tree (including development dependencies), the resolved backend CycloneDX JSON SBOM (including test scope), and the packaged executable JAR; npm audit uses the official registry and a high severity threshold. Maven CycloneDX2.9.1 and npm sbom emit SBOMs. Actions are pinned to verified v4 commit SHAs and scanner images to the digests observed in the successful source CI run. Registry rules and vulnerability databases require fresh network updates; there is no continue-on-error or stale-feed fallback. An empty/missing scan report or scanner error is not a pass.

CI must have read-only repository permissions and no production secrets. Protect the default branch with all three jobs required, review and no force-push. OWASP Dependency-Check12.1.8 is retained in a separate manual supplemental workflow, still failing on CVSS>=7 or scanner errors. The previous required job stalled bootstrapping 417,614 NVD records without a key; the source-preview gate now uses fresh Trivy scanning of both resolved dependencies and the real artifact instead of depending on an NVD API key. This changes the advisory engine and is not evidence that the unfinished NVD scan passed. NVD_API_KEY, if configured for the supplemental run, is never a deployment credential. Audit the full transitive license inventory; RuoYi is design reference only unless copied code/license is explicitly recorded.

ASVS mapping for review: authentication/session (V2/V3 in ASVS4 terminology), access control(V4), validation/output(V5), stored crypto(V6), errors/logging(V7), data protection(V8), files(V12), APIs(V13), configuration(V14). Record the exact ASVS edition/control IDs in the release assessment; this category mapping is not a certification. Required regression cases are in the [acceptance matrix](../testing/acceptance-matrix.md).

Incident response: contain the affected listener/account, preserve audit and encrypted backups, rotate exposed credentials/keys, invalidate sessions/recovery material, review cross-company access and failed jobs, restore only after verifying immutable KPI/attachment history. Never log secrets to investigate them.
