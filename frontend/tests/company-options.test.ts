import { describe, expect, it } from 'vitest'
import { availableTaskCompanies, departmentsForCompany, companyLabel } from '../src/domain'

const departments = [
  { id: 10, companyId: 1, companyName: '甲公司', name: '甲部门' },
  { id: 11, companyId: 1, companyName: '甲公司', name: '甲二部' },
  { id: 20, companyId: 2, companyName: '乙公司', name: '乙部门' }
]

describe('task organization choices', () => {
  it('lists companies with available departments for a group leader', () => {
    expect(availableTaskCompanies('GROUP_LEADER', null, departments)).toEqual([
      { id: 1, name: '甲公司' }, { id: 2, name: '乙公司' }
    ])
  })

  it('limits a company leader to the active company rather than other scopes', () => {
    expect(availableTaskCompanies('COMPANY_LEADER', '1', departments)).toEqual([
      { id: '1', name: '甲公司' }
    ])
    expect(departmentsForCompany('1', departments).map(d => d.id)).toEqual([10, 11])
    expect(departmentsForCompany(null, departments)).toEqual([])
  })
})

it('shows the name for numeric or string company identifiers, including disabled companies', () => {
  const companies = [{ id: 3, name: '示例公司', enabled: false }]
  expect(companyLabel('3', companies)).toBe('示例公司')
  expect(companyLabel(9, companies)).toBe('公司名称不可用')
  expect(companyLabel(null, companies)).toBe('—')
})