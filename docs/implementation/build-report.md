# 全量构建续跑检查点（更新至2026-10-26）

项目位于 `D:/TASK/task-workbench`，分支 `codex/v1.5-build`。用户恢复全量构建后，本轮继续处理上一检查点的环境门禁并补充可审查证据；这仍是本地开发检查点，尚未取得生产发布验收。

## A. 已完成 / 未完成

完成岗位任职有效期、离岗状态、限时部门负责人代理、显式代理上下文、批量责任交接与原责任快照；完成公司版本化工作日历、节假日、请假排除、复杂度分类、候选权重对比和冻结模拟；完成真实 XLSX/PDF/CSV 导出与对应前端，保留正式 KPI 原有30/25/20/10/15权重及历史结果。

此前主体工程已覆盖组织、Session/CSRF/TOTP、任务状态流转、延期/撤回/返工、跨公司协办、附件隔离、规则匹配、事务发件箱、月度考核/申诉、管理页面和部署备份模板。本轮同时修复重新分派绕过交接记录、过期外公司任职绕过账号管理授权、代理身份切换后的幂等重放等问题。详细范围见各实现报告；不能据此宣称42项全部验收。

尚未完成的环境验收：V1–V9虽已由MySQL8.4.13空库引擎直接执行成功，但Flyway/JDBC及当前增量HTTP流程仍受本机进程间通信限制；真实浏览器/企微设备、完整后端安全扫描/SBOM及许可证审查、50用户性能、Linux/Nginx HTTPS部署、生产权限/告警、实际企微投递、加密备份/binlog/PITR恢复演练仍未完成。历史库升级、最小权限迁移身份、大数据SQL分页及EXPLAIN也需补充。

## B. 文件清单

- 后端：`OrganizationChanges`及Controller、`CalendarPolicy`及Controller、`ExportFormats`、`StatsController`；必要的Policy/Auth/Store/Writes/TaskService/OrganizationController补丁；仅追加V8/V9。
- 前端：`OrganizationChanges.vue`、`CalendarSimulation.vue`、表单校验/测试、角色导航和路由；KPI/统计导出格式；重新分派原因。
- 文档：ADR-001/003、API草案、数据字典、42项验收矩阵、各实现报告、测试证据及运行库哈希清单。
- 启动脚本：演示TOTP密钥使用本机Windows DPAPI保护，跨终端重启可复用；保留loopback、独立H2和进程身份检查。PDF使用配置的可信中文TTF，演示可使用本机SimHei，不分发该字体。

## C. API 与迁移

API均位于 `/api/v1`，新增 `/organization/appointments`、`/organization/delegations`、`/organization/handovers`、`/kpi/calendar-*`、`/kpi/simulation-templates`、`/tasks/{id}/classification`。`/stats/export`及`/kpi/export`支持format=xlsx/pdf/csv，要求近期TOTP，限制行数、页数、频率和并发。OpenAPI草案现有85条路径、108个操作；部分Map请求尚需完整DTO/字段契约，不以清单数量代替接口验收。

V8添加任职/代理/交接及审计代理标识，V9添加独立候选日历/分类/模板/模拟表。未修改既有V1–V7。正式KPI未采用新候选策略；涉及生产数据时仍需预备份、MySQL迁移预演及授权发布窗口。

## D. 实际验证

| 检查 | 本轮结果 |
|---|---|
| `mvn ... -f backend/pom.xml -B -ntp package` | 101项测试，0失败/错误/跳过；可执行JAR生成成功 |
| 技术组合 | Corretto21.0.13、Boot3.5.16、MyBatis Starter3.0.5、Flyway11.20.3；本轮H2 V1–V9迁移成功 |
| 前端Node24.22.1 | 类型检查、ESLint、20项Vitest、Vite生产构建通过 |
| 独立导出解析 | openpyxl检查XLSX文本/公式安全；pypdf核对中文、85.83、N/A、水印；PDF已渲染目检 |
| 脚本/API静态检查 | PowerShell解析、JSON格式、42项验收条目及部署模板一致性检查通过；静态检查不等于部署成功 |
| 干净目录 `npm ci --offline` | 常规安装245项依赖成功且未改锁文件；随后类型检查、ESLint、20/20 Vitest和生产构建全部通过 |
| 前端依赖审计/SBOM | `npm audit --offline`报告0项漏洞；npm CycloneDX 1.5清单包含244个组件、245项依赖；仍需在线刷新与许可证审查 |
| MySQL8.4.13空库V1–V9 | 引擎直接执行未修改迁移SQL成功：40张表、V8/V9的6张表、6个关键字段、5个索引、14个CHECK约束；不是Flyway/JDBC或历史升级验收 |
| 本地H2演示启动器 | Java21.0.2/Node22.23.4下启动成功；V1–V9经Flyway执行，后端健康、前端、CSRF、员工登录/会话/首次改密门禁/退出均通过。修复PowerShell空环境值屏蔽基础配置、账号初始化就绪竞态和UTC进程身份判断；不等同于生产验收 |
| 当前浏览器实测 | 缓存Chromium以真实本地API完成员工登录、首次改密页面和退出；桌面页面及375/768宽度无页面错误或水平溢出。此前45组合成fixture证据仍只代表fixture，本次未重跑全部45组真实API流程 |
| 当前本地HTTP/MySQL复测 | 本地H2 HTTP流程已验证；MySQL8.4引擎证据已取得，但MySQL Flyway/JDBC、历史升级及生产部署流程仍未验证 |
| 后端扫描/SBOM | 缓存中的CycloneDX插件缺少依赖描述符；OWASP离线数据不可用且更新时间为2026-08-07，未形成可接受报告 |

保留而不扩大旧证据：2026-10-24的MySQL8.4.13中间V1–V5版本已通过13项HTTP流程；当日合成API浏览器fixture已通过35组页面/视口检查。二者均不证明本轮新增代理、日历、导出或V8/V9已通过真实环境验收。前端主包约1111kB、gzip370kB，构建保留大包警告。

可审查的结构化证据见 `docs/testing/evidence/`，其中新增MySQL V1–V9引擎证据和前端干净构建证据。运行库清单为 `docs/release/runtime-dependency-inventory.json`（不是已审查的后端SBOM）。

## E. 安全与数据

代理只限明确公司/部门/时窗/操作，禁止代理管理员或集团领导权限及转授权；每请求重查双方任职和撤销状态。转交保留实际操作者、理由、原/新责任人快照，不能重写成果或历史KPI。管理员修改共享账号使用包括过期任职在内的完整范围检查。

导出在服务端重新授权，XLSX单元格使用inlineStr而非公式；PDF/表格携带导出人、上下文和时间。文件/测试数据库/密码/TOTP密钥/Cookie/npm缓存/后台日志不纳入交付包；测试源码中的固定值仅用于合成测试。

## F. 回滚与风险

本地源码保留独立Git检查点。数据库不能自动执行破坏性down迁移；上线前必须验证旧程序兼容性，必要时在隔离环境恢复一致备份并核对附件/KPI/发件箱。当前未访问真实业务库、未推送或合并远程分支、未发布站点。

## G. 当前续跑位置

2026-11-01续跑：在独立本机MySQL 8.4.13空库上，应用通过JDBC连接并由Flyway成功执行V1–V9；迁移历史9项成功，含Flyway历史表共41张表。健康检查、CSRF、员工登录、会话及首次改密标志通过。证据见 `docs/testing/evidence/mysql84-flyway-jdbc-20261101.json`。该验证使用临时本地root账号和合成数据，不代表历史库升级、最小权限、并发或生产验收。此前表中MySQL JDBC未验证的描述保留为当时结果，由本段更新。

后续仍需完成完整45组真实API UI门禁，并在可更新漏洞库的CI环境完成后端扫描和SBOM，再依据42项差异清单继续。
