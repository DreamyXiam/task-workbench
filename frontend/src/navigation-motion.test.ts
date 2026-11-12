import {describe,expect,it} from 'vitest'
import {navigationMotion} from './navigation-motion'
describe('菜单切换动效',()=>{
 it('工作空间和系统管理内部滑动，跨组淡入淡出',()=>{
  expect(navigationMotion('/tasks','/collections','ADMIN')).toBe('page-slide')
  expect(navigationMotion('/admin/companies','/admin/users','ADMIN')).toBe('page-slide')
  expect(navigationMotion('/tasks','/admin/companies','ADMIN')).toBe('page-fade')
  expect(navigationMotion('/admin/users','/tasks','ADMIN')).toBe('page-fade')
 })
 it('领导的操作留痕属于工作空间，相同页面不触发动效',()=>{
  expect(navigationMotion('/tasks','/admin/audit-logs','COMPANY_LEADER')).toBe('page-slide')
  expect(navigationMotion('/tasks','/tasks','ADMIN')).toBe('page-none')
 })
})
