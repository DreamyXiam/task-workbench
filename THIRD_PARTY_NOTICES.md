# Third-party notices

Task Workbench's original code is licensed under AGPL-3.0-only. Dependencies retain their own licenses; this file does not relicense them.

The version-specific inventories, CycloneDX SBOMs, extracted upstream notices, and review limitations are in [docs/release/licenses](docs/release/licenses/REVIEW.md). The inventories include development/test dependencies as well as runtime dependencies. A dependency appearing here does not mean its source or binary is redistributed in this repository.

Frontend dependency metadata declares MIT, Apache-2.0, ISC, BSD-2-Clause, BSD-3-Clause, BlueOak-1.0.0, and Python-2.0 licenses. Backend metadata also includes EPL/LGPL alternatives, MPL/EPL alternatives, CC0/BSD alternatives, and MySQL Connector/J's GPLv2 with Universal FOSS Exception 1.0. Consult each component's original terms; multiple licenses reported by Maven are not automatically an SPDX AND or OR expression.

Particular attention is required for:

- MySQL Connector/J 9.7.0: retain its [complete upstream license](docs/release/licenses/upstream-notices/maven/com.mysql__mysql-connector-j__9.7.0/LICENSE), including GPLv2, Universal FOSS Exception 1.0, and notices for its own third-party materials. The exception is conditional, including complete corresponding source for the other FOSS and use through interfaces; it does not relicense Connector/J under AGPL.
- Logback classic/core 1.5.34: upstream metadata offers EPL-2.0 and LGPL-2.1 terms. A distributor must deliberately choose and comply with the applicable alternative; LGPL source/relinking requirements are not discharged by an SBOM.
- H2 2.2.224: metadata lists MPL-2.0 and EPL-1.0 alternatives. Preserve H2's source and notices under the selected upstream option when redistributing it.
- Jakarta Annotation API 2.1.1: metadata lists EPL-2.0 and GPLv2 with Classpath Exception. Preserve the upstream exception and notices if relying on that option.
- JUnit development/test components: EPL-2.0; these are not in the inspected application JAR.

For binary distributions, ship this file and the relevant upstream license/copyright/NOTICE files with the artifact, preserve existing notices in dependency JARs and browser bundles, and satisfy applicable corresponding-source obligations. A root AGPL license file or a link to this repository alone does not establish compliance for every bundled component.

No tracked font, image, icon, or video files were found in the source tree on 2026-11-01. The frontend refers to locally installed system fonts and draws the watermark using canvas. Referencing a system font by name does not include its font file in this project. Dependency-provided icons and other assets remain covered by the dependency's upstream terms.
