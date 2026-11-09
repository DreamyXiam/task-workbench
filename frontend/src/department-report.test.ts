import {describe,expect,it} from 'vitest'
import {composeDepartmentReport,selectedReportFields} from './department-report'
describe('部门填报项目',()=>{
 it('只保存选择的项目并保持预设顺序',()=>{expect(selectedReportFields(['timeRequirement','progress','unknown']).map(f=>f.name)).toEqual(['推进情况','时间要求'])})
 it('只汇总选中项目中的有效内容，保留零值和多行说明',()=>{
  expect(composeDepartmentReport(selectedReportFields(['progress','problem','timeRequirement']),{progress:' 已完成核对\n等待复核 ',problem:' ',timeRequirement:'11月15日前',secret:'不应写入'})).toBe('推进情况：已完成核对\n等待复核\n时间要求：11月15日前')
  expect(composeDepartmentReport([{id:'count',name:'数量'}],{count:0})).toBe('数量：0')
 })
})
