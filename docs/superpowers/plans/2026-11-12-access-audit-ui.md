# 权限与管理界面 Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement task-by-task.

**Goal:** 使入口、服务端权限及管理界面一致，并交付可筛选导出的审计日志。

**Architecture:** 保留角色与数据范围校验；新增审计查询控制器与独立页面；复用现有导出格式与主题tokens。

**Tech Stack:** Java21、Spring、MyBatis、Vue3、Element Plus。

**Spec:** ../specs/2026-11-12-access-audit-ui-design.md

## Global Constraints

- 不改变公司管理员业务数据边界，不更改审计链。
- 不引入新组件依赖。沿用服务器Java21.0.2/MySQL8.0.45。
- 全局管理员查看考核时不获得领导调分/申诉能力。

## Review Focus

- 全局查询避免将null公司变为0。
- 日志map字段显式映射，不依赖下划线自动转换。
- 公司管理员/公司领导不得通过筛选或导出越权。
- 关闭TASK_VIEW后依赖功能保持禁用；角色不支持项不能看成授权。
- 导出失败与未保存状态要可恢复，不丢失用户输入。

### Task 1: 查看权限

- [x] HttpSecurityTest新增全局统计/考核可读、公司管理员和员工不可读回归，先运行失败。
- [x] Policy.Actor增加报表阅读能力；StatsController和KpiController修正空公司全局阅读及只读动作。
- [x] 前端菜单与canReadKpiScores使用公司范围区分管理员，考核页全局提供公司选择用于计算，读取默认全范围。
- [x] 运行后端回归并提交。

### Task 2: 审计查询与导出

- [x] 新增AuditControllerTest验证字段名称、公司隔离、关键字日期过滤、分页、xlsx和近期验证。
- [x] AuditLogService使用参数查询返回明确映射；AuditController替代原始/audit查询，新增/audit/export与权限映射。
- [x] 新增AuditLogs.vue独立路由，筛选、详情、导出与验证交互。
- [x] 后端验证与前端类型检查并提交。

### Task 3: 权限和岗位UI、统一规范

- [x] 角色支持功能元数据与生效依赖测试先失败，再实现；保留历史disabled列表格式。
- [x] RolePermissions.vue按角色与权限分组、显示支持/生效/依赖、未保存提示和保存确认。
- [x] OrganizationChanges.vue增加摘要、搜索、统一记录样式；共享theme增加工具条与卡片规范。
- [x] 单元、类型、构建及浏览器验证；更新文档。
- [ ] 正常PR/CI/合并、默认部署与真实角色验证。

## 执行证据

全局查看权限红测403，审计分页字段红测缺失，角色支持元数据红测缺失，动作映射及员工协同红测均已确认后修复。后端完整147项、前端57项、类型/构建通过；lint仅8个既有警告。原142项浏览器回归通过，新增管理界面20项通过。独立复查三项问题已修复：实际动作编码映射、公司选择名称、员工协同权限元数据。

整合浏览器162项通过；测试站5角色实测统计/考核权限、日志映射、公司边界、筛选及12字段XLSX导出通过。GitHub SAST对动态查询结构报SQL风险，改为固定SQL模板、所有筛选值继续参数绑定，未降低检查标准。
