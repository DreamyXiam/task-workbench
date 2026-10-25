# 本轮完成与暂停记录（2026-10-25）

本轮已完成当前部分，遵照用户“完成这部分后暂停”的指示停止后续开发。项目位于 `D:/TASK/task-workbench`，分支 `codex/v1.5-build`。这是本地可审查、可构建的开发检查点，尚未取得生产发布验收。

## A. 已完成 / 未完成

完成岗位任职有效期、离岗状态、限时部门负责人代理、显式代理上下文、批量责任交接与原责任快照；完成公司版本化工作日历、节假日、请假排除、复杂度分类、候选权重对比和冻结模拟；完成真实 XLSX/PDF/CSV 导出与对应前端，保留正式 KPI 原有30/25/20/10/15权重及历史结果。

此前主体工程已覆盖组织、Session/CSRF/TOTP、任务状态流转、延期/撤回/返工、跨公司协办、附件隔离、规则匹配、事务发件箱、月度考核/申诉、管理页面和部署备份模板。本轮同时修复重新分派绕过交接记录、过期外公司任职绕过账号管理授权、代理身份切换后的幂等重放等问题。详细范围见各实现报告；不能据此宣称42项全部验收。

尚未完成的环境验收：新V6–V9迁移在MySQL8.4的复验、当前增量的真实浏览器/企微设备验证、完整安全扫描/SBOM及许可证审查、50用户性能、Linux/Nginx HTTPS部署、生产权限/告警、实际企微投递、加密备份/binlog/PITR恢复演练。数据量较大时的SQL分页及EXPLAIN仍需补充。

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
| `npm ci --offline` | Windows mkdir EPERM，未通过；从既有缓存逐一验证SHA512恢复245项适用依赖，未改锁文件 |
| 当前45组浏览器检查 | Chromium启动即0xC0000022，未进入页面，未通过 |
| 当前本地HTTP/MySQL复测 | 沙箱拒绝本地TCP连接，未验证 |

保留而不扩大旧证据：2026-10-24的MySQL8.4.13中间V1–V5版本已通过13项HTTP流程；当日合成API浏览器fixture已通过35组页面/视口检查。二者均不证明本轮新增代理、日历、导出或V8/V9已通过真实环境验收。前端主包约1111kB、gzip370kB，构建保留大包警告。

可审查的结构化证据见 `docs/testing/evidence/`，运行库清单为 `docs/release/runtime-dependency-inventory.json`（不是已审查的SBOM）。

## E. 安全与数据

代理只限明确公司/部门/时窗/操作，禁止代理管理员或集团领导权限及转授权；每请求重查双方任职和撤销状态。转交保留实际操作者、理由、原/新责任人快照，不能重写成果或历史KPI。管理员修改共享账号使用包括过期任职在内的完整范围检查。

导出在服务端重新授权，XLSX单元格使用inlineStr而非公式；PDF/表格携带导出人、上下文和时间。文件/测试数据库/密码/TOTP密钥/Cookie/npm缓存/后台日志不纳入交付包；测试源码中的固定值仅用于合成测试。

## F. 回滚与风险

本地源码保留独立Git检查点。数据库不能自动执行破坏性down迁移；上线前必须验证旧程序兼容性，必要时在隔离环境恢复一致备份并核对附件/KPI/发件箱。当前未访问真实业务库、未推送或合并远程分支、未发布站点。

## G. 暂停位置

本轮实现、可执行构建和结果说明保存后暂停。后续恢复时，应先处理上表明确未验证的环境验收，再依据42项差异清单决定下一项；本轮不会自行启动新的开发或部署任务。
