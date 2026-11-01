# Open-source preview readiness

Updated 2026-11-01. Scope: publication of this source repository as a development preview. This does not replace the V1.5 production release gates and does not authorize public visibility or a binary release.

## Completed source preparation

- [x] Owner confirmed publication rights for all project code and business documentation and selected AGPL-3.0-only.
- [x] Complete license text, bilingual README, contribution/security instructions and PR template are present.
- [x] Frontend/backend direct and transitive dependency inventories, SBOMs and upstream notices reviewed. See [license review](licenses/REVIEW.md). This is a source-only preview; binary redistribution conditions remain separate. No font binaries are distributed.
- [x] All remotely advertised refs inventoried with git ls-remote: one working branch, no tags or other advertised refs. No releases, issues or pull requests were present at review time. Root and remote commit identity were checked before each push; all reachable publication history was scanned with Gitleaks, retaining only existing exact synthetic-test exceptions.
- [x] Tracked source, history, SQL, docs and final synthetic screenshots reviewed; see [privacy review](privacy-review.md). Three obsolete provenance identifiers were removed from the current document. Non-secret historical identifiers and maintainer account linkage remain in history deliberately; no history rewrite occurred.
- [x] Clean source directory: fresh npm ci, frontend lint/typecheck/20 tests/build; backend Java21/Maven clean verify and subsequent 102-test patched-backend verify passed. Existing local runtimes were reused without installing/upgrading them.
- [x] Fresh isolated H2 demo against the patched JAR: 24 real HTTP/browser checks, desktop/375px/768px, password rotation/re-login/logout, no horizontal overflow/page errors, and clickable mobile source link. Screenshots/evidence are [recorded](../testing/evidence/oss-clean-demo-20261101.json); identity-checked cleanup stopped the demo.
- [x] Network source link added, configurable at build time with VITE_SOURCE_URL; [operators must point it at their actual deployed source](source-distribution.md).
- [x] Actions reviewed: read-only permissions, reviewed action commit/image digest pins, bounded job timeouts, no production secrets or privileged pull_request_target trigger, restricted artifact paths and 14-day retention. Secrets/static-analysis/source scanner jobs have completed online; final backend scan results are recorded separately below.
- [x] Jackson patched to 2.21.7 and all embedded Tomcat components to 10.1.61 following the resolved dependency scan. Current SBOMs, notices and actual-JAR inventory refreshed.
- [x] Two Spring MVC advisories evaluated against official feature prerequisites. The [specific VEX assessment](../security/spring-mvc-vex-review.md) preserves original findings, exact application/library scope, an application-context guard and a review deadline; it does not claim Spring is patched. Owner review accompanies public-visibility approval.
- [x] Private contact verified against the maintainer's publicly displayed profile: wordpressxiam@qq.com. SECURITY.md includes the address; no test email or security message was sent.

## Final cloud evidence

The required workflow checks clean builds, online npm audit, all-history Gitleaks, Semgrep OWASP rules, source/config scanning including development dependencies, resolved backend SBOM including test dependencies, and the actual executable JAR. Findings at HIGH/CRITICAL fail except the two exact application-specific not-affected VEX statements. Unfiltered backend advisory evidence is retained alongside the gated report.

The official npm registry reported 13 moderate development-only findings through minimatch (GHSA-g3hv-75wg-m249), zero high/critical, and zero production dependency findings. This is not a zero-vulnerability claim. npm's suggested fixes downgrade incompatible tool versions; no blind audit fix or severity suppression was applied. Existing job timeouts limit untrusted development input processing. Reassess on dependency/advisory updates.

The previous keyless NVD bootstrap did not complete and is not counted as a pass. Required checks now use fresh Trivy scans of resolved dependencies and the packaged artifact. OWASP Dependency-Check remains in an independent manual supplemental workflow with its original CVSS>=7 failure behavior.

Final required-cloud status and tested commit: see the accompanying source-preview security evidence and GitHub Actions run. No pending/scanner-error result may be represented as a pass.

## Verified public repository settings

The repository was observed public on 2026-11-01; this task did not change its visibility. The former private-repository plan restriction no longer blocks settings. After the owner completed GitHub's required sudo reauthentication:

- [x] Private vulnerability reporting enabled; reloaded settings show Disable, and the anonymous [security advisories page](https://github.com/DreamyXiam/task-workbench/security/advisories) exposes Report a vulnerability. No test report/email was submitted.
- [x] Branch rule 85658454 created for codex/v1.5-build; API branch metadata confirms protected=true. Read-back requires backend/frontend/security checks, an up-to-date branch, a PR, resolved conversations, and enforcement for administrators. Force-push and deletion remain disabled. Zero required external approvals accommodates the solo maintainer without bypassing CI.
- [x] Anonymous repository/README, AGPL license and screenshot access verified; raw license/image returned HTTP200. Source link now resolves to the publicly readable project.

The [prepared configuration](github-publication-settings.json) records the intended rule and verified settings. The connector cannot read the administration-only protection endpoint, so detailed read-back used the authenticated GitHub settings UI; the protected flag was independently confirmed through repository branch metadata.

The complete source candidate at 6f1236d43ba34523088eecc72d32b51e670d6d14 passed [all three required CI jobs](https://github.com/DreamyXiam/task-workbench/actions/runs/41290452022). The log confirms 102 backend tests, 98 resolved scan components, and 62 actual-artifact scan components. Two original Spring findings remain in the raw reports and receive the exact reviewed application-specific not-affected decision. Frontend production dependencies have no audit findings; development-only moderate findings remain disclosed above.

No paid upgrade, release/tag, binary distribution or OSS application was performed. Source-preview publication and production acceptance remain distinct. The final documentation update uses the protected branch's normal PR/check workflow.
