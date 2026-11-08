import { describe,expect,it } from 'vitest'
import { departmentProgress,latestProgress,visibleProgress } from './detail-progress'

describe('任务详情进展',()=>{
 it('按时间选择最新进展，不改变原始记录',()=>{
  const entries=[{note:'较新',createdAt:'2026-11-08T08:00:00Z'},{note:'较早',createdAt:'2026-11-07T08:00:00Z'}]
  expect(latestProgress(entries)?.note).toBe('较新')
  expect(entries[0].note).toBe('较新')
  expect(latestProgress([{note:'先',createdAt:'2026-11-08T08:00:00Z'},{note:'后',createdAt:'2026-11-08T08:00:00Z'}])?.note).toBe('后')
 })
 it('没有真实百分比时不编造成零',()=>{
  expect(visibleProgress(null)).toBeNull()
  expect(visibleProgress(undefined)).toBeNull()
  expect(visibleProgress(0)).toBe(0)
  expect(visibleProgress(55)).toBe(55)
 })
 it('区分默认零进度和实际填报的零进度',()=>{
  const sub={id:1,percent:0}
  expect(departmentProgress(sub,[])).toBeNull()
  expect(departmentProgress(sub,[{subTaskId:1,percent:0}])).toBe(0)
  expect(departmentProgress({id:1,percent:100},[])).toBe(100)
 })
})
