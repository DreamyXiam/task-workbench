# Spring MVC application-specific VEX review

Reviewed 2026-11-01 for `cn.workbench:task-workbench:1.5.0-SNAPSHOT` using Spring MVC 6.2.19. This is an application configuration assessment, not a declaration that the library itself is patched. Review again by 2026-12-01 or immediately when dependencies, MVC/view configuration, handlers or deployment extensions change.

| Advisory | Official exploitation conditions | Current application evidence |
| --- | --- | --- |
| [CVE-2026-47884](https://spring.io/security/cve-2026-47884/) | XsltView plus a catch-all mapping that renders a view without an explicit name | No XsltView/XsltViewResolver beans or application XSLT configuration; application handlers are RestController response-body endpoints |
| [CVE-2026-47890](https://spring.io/security/cve-2026-47890/) | View fragments sent over SSE, including attacker-controlled data | No application SSE/fragment-rendering endpoints or emitters |

Official Spring ratings are medium and low, respectively; the scanner's database reports critical. The assessment relies on the published exploitation conditions, not on lowering the scanner severity. Official fixed Spring 6.2.20 is enterprise-only; blindly inserting Spring 7 into Boot 3 is not a compatible patch strategy.

`SpringMvcExposureTest` loads the actual application, checks absence of XSLT view beans, checks response-body semantics and media/return types for application handlers, and guards main Java sources against direct introduction of the vulnerable features. This guard is not a formal reachability proof: reflection, custom external configuration and new runtime plugins require independent review. It deliberately fails when the documented source assumptions change.

`spring-mvc.openvex.json` scopes two not-affected statements to this application product and the exact Spring MVC 6.2.19 subcomponent. It must not be copied to a different application or applied to arbitrary versions. Other advisories remain blocking. CI retains an unfiltered SBOM report and a gated report with VEX, so the original findings remain visible. This is not a global CVE ignore rule, a patched-library claim, or production security approval.

The publication owner must review this assessment with the final source-preview evidence before approving public visibility. Production release gates and supported-platform migration decisions remain separate.

Artifact-scanner handling: Trivy's documented Java JAR/rootfs analyzer reports package identities but no dependency graph. Its built-in product/subcomponent VEX matcher therefore cannot apply these statements to that report. The original JAR findings remain visible; `scripts/check-scan-coverage.py` enforces the same decision only after verifying this exact application product, Spring subcomponent, known patched dependency versions, report coverage, and the two exact statement shapes. Every other HIGH/CRITICAL finding fails. No component-only/global VEX is substituted. Missing, malformed or empty reports fail. The preceding application-context test remains required. The SBOM gate uses Trivy's built-in graph-aware VEX matching directly.
