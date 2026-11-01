# Open-source preview readiness

Status: preparation in progress, 2026-11-01. This checklist concerns public source distribution; it does not replace the V1.5 production release gates.

## Agreed scope

The owner confirmed the right to publish all project source and business documentation and selected AGPL-3.0. Use AGPL-3.0-only for the original project code. Retain third-party licenses and notices. Preserve the existing functionality, requirement history and explicit production limitations. Prepare the existing repository rather than creating a separate public fork or rewriting history.

## Before making the repository public

- [x] Owner confirms publication rights and selects the license.
- [x] Include AGPL v3.0 text from GitHub's choosealicense.com license catalog (GNU's text endpoint timed out); original project metadata uses AGPL-3.0-only.
- [x] Add contribution and private security-reporting instructions.
- [ ] Inventory direct and transitive dependency licenses and produce reviewed backend/frontend SBOMs. Verify any redistributed font or asset separately; the PDF CJK font is operator-supplied.
- [x] Run redacted Gitleaks 8.28.0 history detection on all local refs (8 commits): exit 0, no non-exempt findings. GitHub default-branch HEAD matched local HEAD before this change. Remote-only refs have not been inventoried.
- [ ] Review history, documentation, SQL and screenshots for private infrastructure, identifiers, personal information and real business data.
- [ ] Run clean backend/frontend checks and record exact commands, runtimes and exit codes. Refresh online vulnerability/static-analysis gates; unavailable feeds remain pending.
- [ ] Verify demo setup on a clean environment and capture synthetic-data screenshots. Keep completed local checks distinct from unverified production scenarios.
- [ ] Verify GitHub private vulnerability reporting and the maintainer contact channel.
- [ ] Review GitHub Actions permissions/artifacts and branch protection. CI files alone do not establish successful CI or configured protection.
- [ ] Review the final diff and receive explicit approval for public visibility and any release/tag.

## Initial findings

The local checkout and DreamyXiam/task-workbench both pointed to commit 6bd4cdc6e73011650ea104c4100f839ca0fdba06 during the initial inspection. The local origin still used the previous owner path 3290736814/task-workbench; verify the canonical destination before pushing. The working tree was initially clean and HEAD had eight commits. No real .env, private-key, log, document or spreadsheet files matched the tracked-file extension inventory; this is not a full content/history scan. Example environment files are intentionally tracked.

The existing CI defines backend/frontend verification and security jobs. The acceptance matrix explicitly has no release-accepted rows; the build report contains newer local MySQL/Flyway evidence and must be read alongside that matrix. Do not discard these distinctions in public documentation.

## Application evidence

Do not submit the Codex for OSS application while the repository remains private. After publication, document actual users, issues handled, contributions reviewed and releases maintained. Do not present plans as completed automation or upstream usage as this project's adoption. There is no acceptance guarantee.
