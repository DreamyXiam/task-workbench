import {describe,expect,it} from 'vitest'
import {dayNumber,ganttLayout} from './task-gantt'

describe('甘特图时间范围',()=>{
 it('按北京时间换日，拒绝缺失和无效日期',()=>{
  expect(dayNumber('2026-11-08T16:00:00Z')).toBe(dayNumber('2026-11-09'))
  expect(dayNumber('')).toBeNull()
  expect(dayNumber('invalid')).toBeNull()
  expect(dayNumber('2026-02-30')).toBeNull()
 })
 it('同一天保留完整一天，所有条形在坐标范围内',()=>{
  const chart=ganttLayout([{id:1,createdAt:'2026-11-08',deadline:'2026-11-08'},{id:2,createdAt:'2026-11-01',deadline:'2026-12-10'}])
  expect(chart.rows[0].width).toBeGreaterThan(0)
  for(const row of chart.rows){expect(row.left).toBeGreaterThanOrEqual(0);expect(row.left+row.width).toBeLessThanOrEqual(100)}
  expect(chart.ticks.length).toBeGreaterThan(1)
 })
 it('不伪造缺失日期或反向日期的条形，保持原始顺序和数据',()=>{
  const tasks=[{id:1,deadline:'2026-11-08'},{id:2,createdAt:'2026-11-09',deadline:'2026-11-08'}]
  const chart=ganttLayout(tasks)
  expect(chart.rows.map(r=>r.issue)).toEqual(['缺少创建或截止时间','截止时间早于创建时间'])
  expect(chart.rows.every(r=>r.width===0)).toBe(true)
  expect(tasks[0]).not.toHaveProperty('createdAt')
 })
 it('空数据仍有有限坐标，超长时间跨度不生成海量刻度',()=>{
  expect(Number.isFinite(ganttLayout([]).start)).toBe(true)
  expect(ganttLayout([{createdAt:'2020-01-01',deadline:'2040-01-01'}]).ticks.length).toBeLessThanOrEqual(8)
 })
})
