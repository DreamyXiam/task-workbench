import {describe,expect,it} from 'vitest'
import {dayNumber} from './task-gantt'
import {periodWindow,shiftPeriod,clipRange,moveWindow} from './timeline-window'
const day=(value:string)=>dayNumber(value)!
describe('长周期任务时间视窗',()=>{
 it('一年期任务默认仅显示焦点所在月份，包含闰年和跨年',()=>{
  expect(periodWindow('month',day('2026-11-08'))).toEqual({start:day('2026-11-01'),end:day('2026-12-01')})
  expect(periodWindow('month',day('2028-02-29'))).toEqual({start:day('2028-02-01'),end:day('2028-03-01')})
  expect(shiftPeriod('month',day('2026-12-31'),1)).toBe(day('2027-01-01'))
 })
 it('周以周一为起点，日视窗保留小时；前后导航按相应周期移动',()=>{
  expect(periodWindow('week',day('2026-11-08'))).toEqual({start:day('2026-11-02'),end:day('2026-11-09')})
  expect(periodWindow('day',day('2026-11-08')+.5)).toEqual({start:day('2026-11-08'),end:day('2026-11-09')})
  expect(shiftPeriod('week',day('2026-11-08'),1)).toBe(day('2026-11-15'))
 })
 it('跨窗口的期限及汇报截到视窗边界，窗口外的记录不会挤在边缘',()=>{
  const window={start:day('2026-11-01'),end:day('2026-12-01')}
  expect(clipRange(day('2026-01-01'),day('2027-01-01'),window)).toEqual({left:0,width:100,before:true,after:true})
  expect(clipRange(day('2026-10-01'),day('2026-11-01'),window)).toBeNull()
  expect(clipRange(day('2026-12-01'),day('2027-01-01'),window)).toBeNull()
  expect(clipRange(day('2026-11-08'),day('2026-11-09'),window)?.width).toBeCloseTo(100/30)
 })
 it('概览拖动保持视窗长度并限制在全周期内',()=>{
  const bounds={start:0,end:365},window={start:100,end:130}
  expect(moveWindow(window,1000,bounds)).toEqual({start:335,end:365})
  expect(moveWindow(window,-1000,bounds)).toEqual({start:0,end:30})
 })
})
