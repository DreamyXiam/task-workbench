export type Row = Record<string, any>
export interface Scope { id:string; role:string; companyId:string; deptId?:string }
export interface Me { id:string; name:string; accountSuffix:string; role:string; companyId:string; deptId?:string; mustChangePassword?:boolean; totpRequired?:boolean; totpVerified?:boolean; totpBound?:boolean; scopes:Scope[]; delegation?:{id:string|number;delegatorId:string|number;companyId:string|number;deptId:string|number;permissions:string[];validUntil:string} }
export interface Page<T=Row> { items:T[]; page:number; pageSize:number; total:number }
export const roles:Record<string,string>={GROUP_LEADER:'集团领导',DIVISION_LEADER:'分管领导',COMPANY_LEADER:'公司领导',DEPT_HEAD:'部门负责人',EMPLOYEE:'员工',ADMIN:'系统管理员'}
export const statuses:Record<string,string>={DRAFT:'待确认',PENDING_ASSIGNMENT:'待确认分配',PENDING_CONFIRM:'待确认',ASSIGNED:'待接收',PENDING_RECEIVE:'待接收',PENDING:'待处理',RECEIVED:'已接收',IN_PROGRESS:'进行中',REWORK:'退回整改',PENDING_ACCEPTANCE_WITH_GAP:'有缺项待验收',SUBMITTED:'待汇总',PENDING_ACCEPTANCE:'待验收',COMPLETED:'已完成',ACCEPTED:'已验收',REJECTED:'已退回',CANCELLED:'已取消',WITHDRAWN:'已撤回',LOCKED:'已锁定',SIMULATION:'模拟',OBJECTION:'异议期',QUARANTINED:'隔离待扫描',CLEAN:'安全可用'}
export const actionLabels:Record<string,string>={'confirm-assign':'确认分配',receive:'接收任务',assign:'分派人员',progress:'更新进度',submit:'提交成果',withdraw:'撤回提交',extend:'申请延期',transfer:'转交任务',summarize:'汇总上报',accept:'验收通过',reject:'退回修改',cancel:'取消任务',deadline:'调整截止日期',urge:'催办',approve:'批准',appeal:'提出申诉',appeals:'提出申诉',adjust:'人工调分',resolve:'处理申诉',unlock:'解锁批次',comment:'发表意见',upload:'上传附件'}
export const allowed=(row:Row|null|undefined,action:string):boolean => Array.isArray(row?.allowedActions) && row.allowedActions.includes(action)
export function validateAssignment(host:string,collaborators:string[]):string { if(!host)return '请选择且仅选择一个主办部门'; if(collaborators.length>2)return '协办部门最多两个'; if(collaborators.includes(host))return '主办部门不能同时为协办部门'; if(new Set(collaborators).size!==collaborators.length)return '协办部门不能重复'; return '' }
export const listItems=(data:Row[]|Page):Row[] => Array.isArray(data)?data:data?.items ?? []
export const display=(value:unknown):string => value==null||value===''?'—': typeof value==='object'?JSON.stringify(value):String(value)
export const date=(value:unknown):string=>value?new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',dateStyle:'medium',timeStyle:'short'}).format(new Date(String(value))):'—'
export const overdue=(row:Row):boolean=>{if(row.everOverdue)return true;if(!row.deadline)return false;const deadline=new Date(row.deadline).getTime(),completed=row.submittedAt??row.summarizedAt;if(completed)return new Date(completed).getTime()>deadline;return !['COMPLETED','ACCEPTED','CANCELLED'].includes(row.status)&&deadline<Date.now()}

export const kpiDisplayScore=(row:Row):number|null=>row.finalScore??row.adjustedScore??row.rawScore??row.score??null
export const canReadKpiScores=(role:string):boolean=>['GROUP_LEADER','DIVISION_LEADER','COMPANY_LEADER','DEPT_HEAD'].includes(role)
export function reworkDepartmentOptions(task:Row):Row[]{const result=new Map<string,Row>();for(const sub of task.subTasks??[]){if(sub.deptId!=null)result.set(String(sub.deptId),{id:sub.deptId,name:sub.deptName??sub.departmentName??'部门 '+sub.deptId})}return [...result.values()]}
