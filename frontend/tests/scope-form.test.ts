import { describe, expect, it } from 'vitest'
import { GLOBAL_SCOPE, normalizeScope, validateScope } from '../src/scope-form'

describe('organization scope form', () => {
  it('submits explicit global administration without company or department', () => {
    const scope={role:'ADMIN',companyId:GLOBAL_SCOPE,deptId:''}
    expect(validateScope(scope,true)).toBe('')
    expect(normalizeScope(scope)).toMatchObject({role:'ADMIN',companyId:null,deptId:null})
  })
  it('does not allow a company administrator to grant global access', () => {
    expect(validateScope({role:'ADMIN',companyId:GLOBAL_SCOPE,deptId:null},false)).not.toBe('')
  })
  it('requires an organization for a company role and a department for staff', () => {
    expect(validateScope({role:'COMPANY_LEADER',companyId:null,deptId:null},true)).not.toBe('')
    expect(validateScope({role:'EMPLOYEE',companyId:1,deptId:''},true)).not.toBe('')
    expect(validateScope({role:'EMPLOYEE',companyId:1,deptId:5},true)).toBe('')
  })
  it('requires global scope for group leaders', () => {
    expect(validateScope({role:'GROUP_LEADER',companyId:1,deptId:null},true)).not.toBe('')
    expect(validateScope({role:'GROUP_LEADER',companyId:GLOBAL_SCOPE,deptId:null},true)).toBe('')
  })
})
