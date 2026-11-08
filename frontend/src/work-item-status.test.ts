import {describe,expect,it} from 'vitest'
import {workItemStatus,workItemWirePercent} from './work-item-status'
describe('子任务状态与既有接口兼容',()=>{
 it('以服务端状态显示，兼容旧记录',()=>{
  expect(workItemStatus({status:'IN_PROGRESS',percent:100})).toBe('IN_PROGRESS')
  expect(workItemStatus({status:'COMPLETED',percent:0})).toBe('COMPLETED')
  expect(workItemStatus({percent:100})).toBe('COMPLETED')
 })
 it('保存进行中保留既有内部数据，标为完成发送既有完成值',()=>{
  expect(workItemWirePercent({percent:45},'IN_PROGRESS')).toBe(45)
  expect(workItemWirePercent({percent:45},'COMPLETED')).toBe(100)
  expect(workItemWirePercent({percent:100},'IN_PROGRESS')).toBe(0)
  expect(workItemWirePercent({percent:NaN},'IN_PROGRESS')).toBe(0)
 })
})
