>2026-10-25 implementation update: the following CDEX-001 register preserves the original audit state. The user later authorized full construction in the isolated `task-workbench` repository. [ADR-001](ADR-001-tech-stack.md) now selects the locally tested3.5 baseline; [ADR-003](ADR-003-complexity-calendar.md) documents simulation-only candidates. Other pending business policies retain V1.4 defaults. The latest checkpoint is [build-report](../implementation/build-report.md); no production release is approved.

# 任务工作台 V1.5｜架构决策记录索引

- 建立日期：2026-10-24（Asia/Shanghai）。
- 创建任务：CDEX-001；本次只建立索引，不替后续任务作正式选型或业务审批。
- 审计依据与限制：[V1.4 / V1.5 差异清单](../audit/v14-gap-analysis.md)。
- 当前未发现业务工程、Git commit、Flyway 迁移；没有现行代码行为可替代文档基线。
- 下列条目均为待处理索引，**不是已经创建、批准或实施的 ADR 文件**。

## 记录与审批规则

状态依次为：待评估 → 提议 → 已接受 / 已拒绝 → 被替代。技术兼容性有实际命令、版本和结果后才能冻结；涉及考核制度、保留期或权限例外，需明确业务批准证据。已有规则继续有效，候选只可分析/模拟，不覆盖历史或自动正式启用。

每份后续 ADR 应记录：编号/标题、日期、状态、问题、现有基线、候选及取舍、决定和批准依据、精确版本或策略版本、兼容与迁移、权限与数据影响、实际验证命令/结果、回滚、关联任务和被替代关系。编号沿用 V1.5 清单；尚未创建的文件不放虚假链接。

## 待决策目录

| 编号 / 预期文件 | 决策问题与当前状态 | 当前保留口径 | 阻断点 / 归属任务 |
|---|---|---|---|
| ADR-001-tech-stack.md | 待评估：Boot 4.1.x/Starter 4.x 与 Boot 3.5.x/Starter 3.x；Java、Node、MySQL 兼容与维护；无 PoC 证据。 | V1.4 工具准备使用 Java 21.0.2、Node 22.23.4、MySQL 8.0.45；不等同 V1.5 版本冻结。 | 阻断工程版本冻结；CDEX-002。 |
| ADR-002-kpi-weights.md | 待业务批准：是否调整正式五指标权重。 | 30/25/20/10/15 作为兼容模板；新模板先模拟，不覆盖历史。 | 阻断新权重正式启用；026～029。 |
| ADR-003-complexity-calendar.md | 待评估：复杂度、工作日/自然日、节假日/请假排除。 | 延续 V1.4 当前定义；对缺乏定义者记录歧义，不自造正式口径。 | 不阻断核心流程；027，影响026/028。 |
| ADR-004-attachment-allowlist.md | 待业务确认：白名单与现代 Office 格式示例冲突。 | 10MB；pdf/doc/xls/jpg/png/zip/txt；不擅自新增 .docx/.xlsx。已有白名单仍须完整安全校验。 | 阻断相应格式开放；014。 |
| ADR-005-retention.md | 待业务确认：普通日志、作废业务追溯、KPI 调整、附件历史、备份副本分别保留多久。 | 普通日志 1 年、KPI 调整永久；作废日志永久与附件 30 天/超 5 版归档歧义分别记录；不得启动清理。 | 阻断正式清理策略；015、040。 |
| ADR-006-backup-rpo-rto.md | 待业务批准：RPO/RTO 精确数值及恢复负责人。 | 每日全量+binlog PITR+附件增量；同批次一致备份，不自定指标。 | 阻断生产恢复验收；040。 |
| ADR-007-classification-emergency-access.md | 待业务批准：数据分类分级、管理员业务可见范围与紧急访问。 | V1.4“仅维护”和权限表看板勾选有歧义；采用 V1.5 最小权限，例外访问需明确授权、限时及审计。 | 阻断管理例外授权；008、015。 |
| ADR-008-personal-kpi.md | 待业务批准：个人考核何时正式开启及展示范围。 | 默认关闭、不直接挂钩薪资、首月冷启动。 | 阻断个人正式启用；028～030、036。 |

这些文件名是索引预留，不是本次额外交付。没有生成 ADR-001 正文、兼容性 PoC、DDL 或后续实现。

## CDEX-001 补核对项

| 项目 | 当前处理 | 关闭条件 |
|---|---|---|
| 本轮附件与原名清单一致性 | 原名清单已全文读取，附件正文无法读取/获取；不以同大小推定相同。 | 取得附件可读全文后比对。 |
| 远端 V1.4 PDF 与本地 50 页副本一致性 | 明示大小差异与本地 hash，页码映射仅指本地副本。 | 取得原件后核对功能、表、规则和页码。 |
| 正确源码仓库 / 新建项目属性 | 当前 D:\TASK 为环境工具与设计资料，E:\Projects 为空。 | 如有现成源码先补审；若新建则由后续授权任务建立工程。 |
| 数据库及历史 Flyway | 未连接真实库；没有迁移文件。 | 取得合法测试副本及真实仓库迁移后盘点，禁止改旧迁移。 |
| 工具链可用性 | 脚本测试 22/22 通过；环境检查返回 1，MySQL 服务报告 Missing，Maven 有权限诊断。 | 后续使用隔离测试环境核实；不将工具版本输出当业务构建通过。 |

CDEX-001 之后的任何实现均等待用户明确指定任务编号。

