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

## Settings that require public visibility

GitHub returned 403/upgrade-required for private-repository branch protection. The attempted classic-rule creation was not saved. GitHub private vulnerability reporting is available for public repositories; the current private repository does not expose its enable control. These are platform constraints, not completed settings.

Prepared exact configuration: [github-publication-settings.json](github-publication-settings.json). Immediately after the owner approves public visibility:

1. Enable private vulnerability reporting and verify an unauthenticated public visitor can see the private report entry.
2. Apply branch protection to codex/v1.5-build: require backend/frontend/security checks, up-to-date branch, PR, resolved conversations, no force-push/deletion, and admin enforcement. Zero required external approvals supports the current solo maintainer; it does not bypass CI.
3. Confirm anonymous access to README, license, source link, screenshots and reporting instructions. Re-read effective protection settings. Do not assert these checks before they happen.

No paid account upgrade, public-visibility change, release/tag, or OSS application has been performed. The owner must review the final candidate including the Spring applicability assessment and approve publication; binary packages remain outside this source-preview scope.
