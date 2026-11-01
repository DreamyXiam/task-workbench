# Security policy

Task Workbench V1.5 is a development preview. No production-supported release or security response SLA is currently promised. Review [security controls and outstanding gates](docs/security/security-baseline.md) and the [acceptance matrix](docs/testing/acceptance-matrix.md) before any deployment. Do not use the demo to store real business data.

## Reporting a vulnerability

Do not publish exploit details, credentials or personal data in issues or pull requests. If GitHub's **Security → Report a vulnerability** is available, use that private channel. Otherwise contact the maintainer through the contact information on https://github.com/DreamyXiam and request a private reporting channel before sending sensitive material. Private vulnerability reporting has not yet been verified as enabled.

Include the affected commit, prerequisites, a minimal synthetic reproduction, impact, and suggested mitigation if known. Avoid testing systems you do not own or have permission to assess. Never send production passwords, session cookies, TOTP seeds or real employee records.

Maintainers should reproduce privately, assess affected versions and scope, prepare and test a fix, and coordinate disclosure. Do not claim remediation until the relevant negative authorization/security checks pass. See the security baseline for incident containment and credential rotation.

中文说明：请通过私密渠道报告漏洞，不要在公开 Issue 中粘贴凭据、个人数据或可直接利用的细节。当前为开发预览版本，生产验收及完整供应链审查尚未完成。
