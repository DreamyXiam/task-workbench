import { describe,expect,it } from 'vitest'
import { keyWorkFields,summaryValue } from './key-work'
describe('重点工作汇总',()=>{
 it('不将重命名的字段误当作原字段',()=>{
  const fields=keyWorkFields(),data={fields,values:{progress:'已完成'}}
  expect(summaryValue(data,'progress')).toBe('已完成')
  fields[0].name='预算'
  expect(summaryValue(data,'progress')).toBe('—')
 })
})
