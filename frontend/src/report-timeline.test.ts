import {describe,expect,it} from 'vitest'
import {reportEvents,reportSegments,timePoint,activeSegment} from './report-timeline'

const task={createdAt:'2026-11-08T00:00:00Z',deadline:'2026-11-10T10:00:00Z'}
describe('汇报时间片段',()=>{
 it('保留同一天的时刻和同时汇报，不改变原始历史',()=>{
  const detail={subTasks:[{deptId:1,deptName:'办公室'}],progress:[{id:2,deptId:1,note:'下午汇报',percent:40,createdAt:'2026-11-08T06:00:00Z'},{id:1,deptId:1,note:'早间汇报',percent:0,createdAt:'2026-11-08T01:00:00Z'},{id:3,note:'协办同时汇报',createdAt:'2026-11-08T06:00:00Z'}]}
  const events=reportEvents(detail).events,segments=reportSegments(task,events)
  expect(segments).toHaveLength(3)
  expect(segments[0].events).toEqual([])
  expect(segments[1].events[0].percent).toBe(0)
  expect(segments[2].events).toHaveLength(2)
  expect(events[0].department).toBe('办公室')
  expect(detail.progress[0].id).toBe(2)
  expect(segments.every(s=>s.end>s.start)).toBe(true)
 })
 it('每个 FILL 只显示实际变化的业务字段，不混入配置或未变化内容',()=>{
  const fields=[{id:'progress',name:'推进情况',type:'TEXT'},{id:'problem',name:'存在问题',type:'TEXT'}]
  const detail={keyWorkHistory:[{id:1,status:'CONFIGURE',createdAt:'2026-11-08T00:00:00Z',before:{},after:{fields}},{id:2,status:'FILL',actorName:'负责人',createdAt:'2026-11-08T02:00:00Z',before:{fields,values:{progress:'初稿',problem:'待核对'}},after:{fields,values:{progress:'已核对',problem:'待核对'}}}]}
  const events=reportEvents(detail).events
  expect(events).toHaveLength(1)
  expect(events[0].content).toBe('推进情况：已核对')
  expect(events[0].author).toBe('负责人')
 })
 it('无汇报不虚构进度，错误时间计数并排除，延期后的汇报不会消失',()=>{
  expect(reportSegments(task,[])[0].events).toEqual([])
  const history=reportEvents({progress:[{id:1,note:'错误时间',createdAt:'bad'},{id:2,note:'逾期汇报',createdAt:'2026-11-12T02:00:00Z'}]})
  expect(history.invalid).toBe(1)
  const segments=reportSegments(task,history.events)
  expect(segments.at(-1)!.end).toBeGreaterThan(timePoint('2026-11-12T02:00:00Z')!)
  expect(activeSegment(segments,timePoint('2026-11-12T02:00:00Z')!)?.events[0].content).toBe('逾期汇报')
  expect(activeSegment(segments,timePoint('2026-11-07T00:00:00Z')!)).toBeUndefined()
 })
 it('相同内容的不同汇报仍分段，边界时刻选择新的汇报',()=>{
  const events=reportEvents({progress:[{id:1,note:'继续核对',createdAt:'2026-11-08T01:00:00Z'},{id:2,note:'继续核对',createdAt:'2026-11-08T02:00:00Z'}]}).events
  const segments=reportSegments(task,events)
  expect(activeSegment(segments,events[1].at)?.events[0].id).toBe('progress:2')
  expect(segments).toHaveLength(3)
  expect(reportSegments({createdAt:'bad',deadline:task.deadline},events)).toEqual([])
 })
})
