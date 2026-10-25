# ADR-001: Local integration baseline and remaining release gates

Updated2026-10-25 (Asia/Shanghai). Decision: select Boot3.5.16 + MyBatis Starter3.0.5 for this development branch. Production release approval remains pending.

| Component | Tested local version | Evidence and limit |
|---|---|---|
| Java | Amazon Corretto21.0.13+10-LTS | Actual compiler/tests/package. Local official archive ZIP CRC checked; vendor detached signature was not independently verified. |
| Maven |3.9.12|Actual offline repeat runs using dependencies previously resolved from Maven Central.|
| Backend |Spring Boot3.5.16, MyBatis Starter3.0.5|69 full tests passed before the checkpoint additions; later focused export tests6passed. Final checkpoint evidence is in build-report.|
| Flyway |11.20.3|H2 V1-V7 migration and full tests passed. Earlier real MySQL V1-V5 smoke used the preceding Flyway11.7.2; do not conflate them.|
| PDF |Apache PDFBox3.0.8|Pinned after official release check;6focused export tests passed; PDF CJK requires an operator-supplied licensed TTF.|
| MySQL |8.4.13|Official ZIP MD5 matched; fresh isolated instance migrated V1-V5 and13HTTP checks passed on2026-10-24. Later V6+ migrations still require MySQL verification.|
| Node/npm |24.22.1 /11.19.1|Official Node ZIP SHA256 verified; frontend checks previously passed. Clean npm ci on2026-10-25 failed on Windows EPERM; integrity-checked cache extraction recovered245applicable packages, not a ci pass.|
| UI |Vue3.5.44, TS5.9.3, Pinia3.0.3, Element Plus2.14.7, Vite7.3.7|Exact direct/transitive pins and integrities are in frontend/package-lock.json.|

The second candidate Boot4.1.2 + Starter4.0.0 was attempted against an isolated copy of the application. Compilation failed because of the Jackson2 ObjectMapper/TypeReference imports and the changed Boot servlet-security auto-configuration package. This is application migration work, not an assertion that Boot4 is incompatible with Java21. Selecting3.5 avoids rewriting those integrations in this branch. The cached Boot3.3 offline profile was removed from the final POM; `verify.ps1 -Offline` changes only network usage, never versions.

On2026-10-24 the selected3.5 candidate passed actual Session/CSRF/Mapper/domain tests, and a MySQL8.4 instance passed the13named HTTP smoke scenarios. A browser fixture passed35route/viewport checks; it uses synthetic responses and is separate from live backend proof. The2026-10-25 environment blocks localhost sockets, so later live-network reruns are explicitly unverified.

Before production freeze: clean Maven/npm CI, complete dependency advisory/license/SBOM review (the npm bulk advisory check returned zero known advisories on2026-10-24, not a perpetual guarantee); final MySQL migrations/concurrency/EXPLAIN; HTTPS and real device browser authentication/download; Linux service, backup/PITR, load and recovery drills; reviewed exact runtime/OS/artifact digests. The build has no production sign-off.

Official references checked2026-10-24:

- https://docs.spring.io/spring-boot/3.5/system-requirements.html
- https://docs.spring.io/spring-boot/system-requirements.html
- https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/
- https://nodejs.org/en/blog/release/v24.22.1
- https://dev.mysql.com/doc/refman/8.4/en/mysql-releases.html
- https://pdfbox.apache.org/download.html (3.0.8)
