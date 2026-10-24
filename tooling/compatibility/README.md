# Candidate verification
These isolated candidate descriptors test dependency availability without changing the project default or local cache.
Run Maven validate offline first; absence is a BLOCKED result, not an incompatibility finding.
Official requirements reviewed2026-10-24:
- https://docs.spring.io/spring-boot/3.5/system-requirements.html (3.5.16)
- https://docs.spring.io/spring-boot/system-requirements.html (4.1.2)
- https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/
- https://nodejs.org/en/blog/release/v24.22.1
Both candidates still need MySQL8.4 migration, Mapper, Session login/CSRF and Actuator integration tests before ADR001 can be accepted.
cache-settings.xml uses repository ID matching existing downloaded metadata; Maven is run -o, no remote mirror is contacted.
