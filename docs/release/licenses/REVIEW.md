# Dependency license and SBOM review — 2026-11-01

This is a dependency metadata and artifact inspection, not a legal opinion or a blanket compatibility certification. No dependency or runtime version was changed. Original-code publication rights and the AGPL-3.0-only selection were confirmed separately by the owner.

## Evidence and coverage

| Evidence | Coverage |
| --- | --- |
| `frontend-inventory.csv` | All 295 non-root lockfile locations, including direct, transitive, development, and platform-optional packages; no missing declared license |
| `frontend-sbom.cdx.json` | npm CycloneDX 1.5 SBOM: 294 unique package/version components and dependency graph; estree-walker 2.0.2 occurs at two locations in the lockfile and is deduplicated |
| `backend-sbom.json`, `backend-sbom.xml` | CycloneDX Maven plugin 2.9.1, 97 resolved components, test scope included, dependency graph, hashes, license metadata/text |
| `backend-inventory.csv` | 98 components: the 97 resolved components plus Spring Boot's packaging-injected jarmode-tools; direct/transitive relationship and actual JAR membership |
| `inventory-verification.json` | Input hashes, inspected application JAR SHA-256, count reconciliation, extraction limitations |
| `upstream-notices/` | Verbatim notices extracted from installed npm packages, integrity-verified npm tarballs, and Maven JARs; includes MySQL's full licensing manual |
| `npm-notice-fetch-verification.json` | Exact tarball sources and successful SRI checks for 64 supplemental notice searches |
| `pom-license-metadata/` | Selected nontrivial Maven license declarations, including tool-supplied standard license texts; these are metadata, not proof of upstream copyright notices |

The inspected application JAR contains 59 dependency JARs. All reconcile to the inventory: 58 are in the Maven dependency SBOM; `org.springframework.boot:spring-boot-jarmode-tools:3.5.16` is injected by Spring Boot packaging and is separately inventoried, with its Apache license and NOTICE preserved. Maven starter/test components not present in this JAR still appear in the full dependency inventory. The SBOM describes resolved dependencies, not a comprehensive decomposition of every vendored/shaded source file inside every dependency.

Frontend declared-license counts by lockfile location: MIT 256; Apache-2.0 16; ISC 9; BSD-2-Clause 8; BSD-3-Clause 4; BlueOak-1.0.0 1; Python-2.0 1. All backend components also declare at least one license. License spellings and alternatives are preserved rather than converted into an unsupported compatibility verdict. `development` and `optional` in the frontend inventory are npm lockfile flags, not a claim that every non-dev file reaches the browser bundle.

## Review result and conditions

The **direct and transitive dependency inventory/SBOM metadata gate is complete**. No component has an unknown declared license. The permissive license families require preserving their specified notices and terms. BlueOak and Python declarations are included explicitly rather than silently grouped into MIT. The root license applies to project code, not dependencies.

The nontrivial copyleft/exception declarations are called out in the root [THIRD_PARTY_NOTICES](../../../THIRD_PARTY_NOTICES.md). In particular, the exact Connector/J 9.7.0 LICENSE includes Universal FOSS Exception 1.0, which expressly permits interface-only interoperability and distribution with other FOSS under their respective terms, provided its conditions are met. This is evidence of the applicable exception, not an unconditional GPLv2/AGPL compatibility claim. Preserve the complete manual and meet source requirements if distributing the driver. No application code was copied from the driver during this review.

**Remaining distribution conditions:** before shipping application binaries, provide the applicable corresponding source for copyleft components and project code, retain copyright/NOTICE/license material with the delivery, and verify the actual final artifact against these inventories. The extraction log identifies packages/JARs without a matching embedded license file. Supplemental npm tarballs were fetched and SRI-verified; some also contain no standalone notice file. Their declared licenses are known, but metadata alone does not prove complete attribution. Do not describe this evidence set as a complete binary-compliance sign-off. Publishing this source repository with dependency manifests is distinct from redistributing dependency binaries or a node_modules archive.

No tracked image/font/media files were found using `git ls-files` for ttf/otf/woff/woff2/png/svg/jpg/jpeg/gif/ico. No such resources occurred in the inspected application JAR. Frontend source uses system fonts and a canvas-generated watermark; no external font download was found in the inspected source. This does not cover a later binary build or every internal asset in third-party packages.

## Reproduction

Use the existing Java 21, Node 24.22.1/npm 11.19.1, and Maven 3.9.16 already present for this task; commands do not upgrade project dependencies. From repository root, set `JAVA_HOME` to the existing JDK and prepend the existing Node directory to PATH as needed:

```powershell
& E:/apache-maven-3.9.16/bin/mvn.cmd -B '-Dmaven.repo.local=D:/TASK/task-workbench/.cache/m2' -s tooling/compatibility/cache-settings.xml -f backend/pom.xml org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom '-DoutputDirectory=../docs/release/licenses' '-DoutputName=backend-sbom' '-DincludeTestScope=true' '-DincludeLicenseText=true'
& .cache/node24/node-v24.22.1-win-x64/node.exe .cache/node24/node-v24.22.1-win-x64/node_modules/npm/bin/npm-cli.js sbom --sbom-format=cyclonedx --package-lock-only --prefix frontend > docs/release/licenses/frontend-sbom.cdx.json
```

Maven completed with BUILD SUCCESS. Its validator emitted warnings about `meta:enum` and `deprecated` schema keywords, but generated and attached both SBOM formats successfully. JSON parsing, component counts, missing-license checks, lockfile deduplication, and JAR membership reconciliation were performed independently. SBOM timestamps/serial numbers may vary on regeneration. Regenerate and re-review after any manifest/lockfile or distribution artifact change.
