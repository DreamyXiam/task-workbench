# Contributing to Task Workbench

Thank you for helping improve Task Workbench. This is a development preview, not a production-approved release. Start with [README](README.md), [development](docs/development.md), and the [acceptance matrix](docs/testing/acceptance-matrix.md).

## Issues and pull requests

- Report reproducible bugs and propose changes at https://github.com/DreamyXiam/task-workbench/issues once the repository is public.
- Include the commit, runtime versions, steps, expected/actual behavior, and a minimal synthetic example. Remove credentials, personal information, internal URLs and business records from logs and screenshots.
- Discuss changes to company/department scope, task transitions, KPI formulas, audit retention or migrations before implementation. Preserve documented defaults unless the change explicitly updates the requirements.
- Keep pull requests focused. Explain the problem, resulting behavior, verification performed, and remaining limitations. Add regression coverage when changing behavior.
- For suspected vulnerabilities, follow [SECURITY.md](SECURITY.md), not a public issue.

## Local checks

Use Java 21, Maven 3.9+, and Node 24 as described in the README. From the repository root:

```powershell
mvn -B -ntp -f backend/pom.xml verify
Set-Location frontend
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

`scripts/verify.ps1` runs these checks. Its `-Offline` option applies to Maven only; npm still runs `npm ci`. CI additionally runs dependency, secret and static-analysis gates; local build success does not replace them. Use synthetic data and an isolated database. Do not connect verification scripts to production.

## Licensing and third-party work

The project is licensed under AGPL-3.0-only; see [LICENSE](LICENSE). Submit only material you have the right to contribute under that license. Identify copied or adapted third-party code, its source and license; preserve required notices. Dependencies retain their own licenses. Do not add proprietary fonts, company materials, real attachments, secrets or generated build output.

中文贡献说明：请提交可复现的问题和小范围修改，使用虚构数据，列出验证结果及尚未验证部分。涉及权限、KPI 或数据迁移的变更请先讨论；安全问题请按 SECURITY.md 私下报告。
