import type { Row } from './domain'

export const GLOBAL_SCOPE='__global__'
export function normalizeScope(scope:Row):Row {
  return {...scope,companyId:scope.companyId===GLOBAL_SCOPE||scope.companyId===''?null:scope.companyId??null,deptId:scope.deptId===''?null:scope.deptId??null}
}
export function validateScope(raw:Row,isGlobalAdmin:boolean):string {
  const scope=normalizeScope(raw)
  if(!scope.role)return '请选择岗位角色'
  if(scope.companyId==null){
    if(!isGlobalAdmin||!['ADMIN','GROUP_LEADER'].includes(scope.role))return '请选择岗位所属公司'
    if(scope.deptId!=null)return '全局岗位不能指定部门'
  }
  if(scope.role==='GROUP_LEADER'&&scope.companyId!=null)return '集团领导必须选择全局范围'
  if(['EMPLOYEE','DEPT_HEAD'].includes(scope.role)&&scope.deptId==null)return '员工和部门负责人必须选择部门'
  return ''
}
