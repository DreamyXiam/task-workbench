# 任务工作台 V1.5

简体中文 | [English](README.md)

## 开源预览版

本项目面向希望自行部署组织协同工作台的团队，覆盖部门任务分派、跨公司协作、成果验收和可审计的 KPI 快照。当前定位为开发预览版，生产验收仍未完成。

项目原创代码采用 [GNU AGPL v3.0](LICENSE)（仅第 3 版，`AGPL-3.0-only`）。第三方组件保留各自许可证，依赖许可证审查仍待完成。参与开发请阅读[贡献指南](CONTRIBUTING.md)，安全问题请按[安全报告说明](SECURITY.md)私下报告；公开前需完成[开源准备清单](docs/release/open-source-readiness.md)。加入许可证不代表已经公开发布或提供生产支持。

任务工作台是一个覆盖任务生命周期的源代码项目，提供基于作用范围的 Session/CSRF 安全机制、不可变的 KPI 考核结果，以及 Vue 3/TypeScript 工作台。当前代码可供审查，**尚未通过生产环境验收**。实际范围和验证限制请查阅[验收矩阵](docs/testing/acceptance-matrix.md)、[实现与验证记录](docs/implementation/)和[发布检查清单](docs/release/v1.5-release-checklist.md)。

## 环境要求

构建需要安装已修补的 Java 21、Maven 3.9 或更高版本，以及 Node 24 LTS/npm。部署需要 MySQL 8.4 LTS、Linux/systemd/Nginx 和经过认可的 TLS、备份基础设施。本地验证基线见 [ADR-001](docs/adr/ADR-001-tech-stack.md)；生产验收单独进行。

在项目根目录执行构建与检查：

```powershell
mvn -B -ntp -f backend/pom.xml verify
Set-Location frontend
npm ci
npm run lint
npm run typecheck
npm test
npm run build
Set-Location ..
```

`./scripts/verify.ps1` 执行相同的检查；加上 `-Offline` 时，只使用已缓存的依赖，不改变依赖版本。

## 本地演示

本地演示使用独立的 H2 数据库和合成数据。先按上述步骤安装前端依赖，再将示例中的可执行文件路径替换为本机已安装的、已修补的 Java 21 和 Node 24 路径：

```powershell
./scripts/start-demo.ps1 -Demo -JavaPath 'C:\your-jdk-21\bin\java.exe' -NodePath 'C:\your-node-24\node.exe' -MavenPath 'C:\your-maven\bin\mvn.cmd'
```

已安装依赖的本地 H2 演示也接受 Node 22 系列中的 22.23.4 及以上版本，或 Node 24 系列中的 24.22.1 及以上版本。依赖安装和生产环境基线仍为 Node 24；Java 21.0.2/Node 22.23.4 的验证仅适用于本地演示。

启动脚本会构建后端，安全提示输入独立的初始密码；如无演示专用 TOTP 密钥，会生成密钥并用 Windows DPAPI 保护本地副本，然后在后台启动前后端。脚本会检查端口、仅绑定本机回环地址，并将数据库和附件固定在 `.task-storage/demo-<后端端口>/` 下，继承的数据库配置无法将演示指向 MySQL。脚本会输出访问地址、进程 ID、日志位置和停止命令，不打印凭据，也不会安装运行时、服务或依赖。

已构建并验证 JAR 后，可加 `-UseBuiltArtifacts` 复用它。如果默认端口 8080/5173 已被占用，可指定 `-BackendPort 18080 -FrontendPort 15173`；Vite 会连接所选后端端口。

脚本会移除继承的 Spring 配置覆盖项，并在结束时恢复调用进程原有的环境状态。只有账号初始化完成、健康检查就绪后才会报告启动成功。启动配置和进程身份回归脚本位于 `scripts/tests/`；在 Windows PowerShell 5.1 和 PowerShell 7 中停止进程时，均会核对可执行文件和 UTC 创建时间。

打开启动脚本输出的前端地址。空的演示数据库会使用输入的初始密码创建 `demo.leader`、`demo.head`、`demo.employee`、`demo.finance`、`demo.other`、`demo.group` 和 `demo.admin`。所有账号首次登录时必须修改密码；领导及管理员账号随后需要绑定 TOTP。项目不提供固定密码。

使用启动时输出的 `./scripts/stop-demo.ps1 -StateFile '<输出的状态文件路径>'` 停止演示。脚本会在终止进程树前核对进程身份。同一 Windows 用户在同一台机器上重启时会复用受 DPAPI 保护的演示密钥；迁移演示数据库时需私下妥善保留原密钥，冲突密钥会被拒绝。进程状态文件不会保存密钥或密码。

## 部署与业务基线

生产环境需要独立配置外部 MySQL 凭据和密钥、Secure Cookie、固定的 HTTPS 同源地址、受限文件访问权限，并禁止运行时账号拥有迁移权限。`.env.example` 列出配置项名称，不会自动加载。请查阅[部署与回滚](docs/operations/deployment.md)、[备份与时间点恢复](docs/operations/backup-restore.md)、[监控](docs/operations/monitoring.md)、[安全基线](docs/security/security-baseline.md)、[开发说明](docs/development.md)和[用户及管理员指南](docs/operations/user-admin-guide.md)。相关脚本默认只做预演，尚未操作真实数据库或生产服务。

现有业务基线为：一个主办部门、0–2 个协办部门、跨公司协办须明确同意、原有五项 KPI 权重为 30/25/20/10/15、异议期为 7 天，个人 KPI 默认关闭。[API 草案及限制](docs/api/contract-notes.md)、[OpenAPI](docs/api/openapi.yaml)、[需求清单](docs/requirements/v15-tasks.md)和 [ADR 登记](docs/adr/README.md)记录了尚待决定的业务事项。新公式和策略在批准前均作为模拟方案。
