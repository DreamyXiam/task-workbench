import { describe,expect,it } from 'vitest'
import { validateTimeWindow, parseCalendarDates, validateCandidateWeights, validateWorkingHours } from '../src/workflowForms'

describe('organization and candidate-calendar form boundaries',()=>{
 it('rejects inverted and invalid appointment windows while allowing an open end',()=>{expect(validateTimeWindow('2026-11-01T09:00:00+08:00','2026-10-31T09:00:00+08:00')).not.toBe('');expect(validateTimeWindow('not-a-date','')).not.toBe('');expect(validateTimeWindow('2026-11-01T09:00:00+08:00','')).toBe('')})
 it('normalizes duplicate calendar dates and rejects impossible leap dates',()=>{expect(parseCalendarDates('2028-02-29, 2028-02-29\n2028-03-01')).toEqual(['2028-02-29','2028-03-01']);expect(()=>parseCalendarDates('2026-02-29')).toThrow();expect(()=>parseCalendarDates('2026-02-30')).toThrow();expect(()=>parseCalendarDates('2026/02/01')).toThrow()})
 it('accepts only five bounded weights totaling100 for candidate comparison',()=>{expect(validateCandidateWeights([30,25,20,10,15])).toBe('');expect(validateCandidateWeights([30,25,20,10,10])).not.toBe('');expect(validateCandidateWeights([100])).not.toBe('');expect(validateCandidateWeights([101,-1,0,0,0])).not.toBe('');expect(validateCandidateWeights([NaN,25,20,10,15])).not.toBe('')})
 it('keeps working hours within the same day and disallows reversed ranges',()=>{expect(validateWorkingHours('09:00','18:00')).toBe('');expect(validateWorkingHours('18:00','09:00')).not.toBe('');expect(validateWorkingHours('24:00','25:00')).not.toBe('')})
})


import { validateHandoverSelection } from '../src/workflowForms'
describe('handover form safeguards',()=>{
 it('requires a reason, target owner and current version',()=>{expect(validateHandoverSelection([{subTaskId:1,userId:2,version:3}],'transfer')).toBe('');expect(validateHandoverSelection([{subTaskId:1,userId:'',version:3}],'transfer')).not.toBe('');expect(validateHandoverSelection([{subTaskId:1,userId:2,version:-1}],'transfer')).not.toBe('');expect(validateHandoverSelection([{subTaskId:1,userId:2,version:3}],' ')).not.toBe('')})
 it('bounds a transaction batch and rejects duplicate subtask selections',()=>{expect(validateHandoverSelection([],'transfer')).not.toBe('');expect(validateHandoverSelection(Array.from({length:101},(_,i)=>({subTaskId:i+1,userId:2,version:3})),'transfer')).not.toBe('');expect(validateHandoverSelection([{subTaskId:1,userId:2,version:3},{subTaskId:'1',userId:3,version:3}],'transfer')).not.toBe('')})
})

import { previousShanghaiMonth } from '../src/workflowForms'
describe('closed simulation period',()=>{
 it('defaults to the previous Shanghai month across a year boundary',()=>{expect(previousShanghaiMonth(new Date('2026-12-31T16:30:00Z'))).toBe('2026-12');expect(previousShanghaiMonth(new Date('2026-12-31T15:30:00Z'))).toBe('2026-11')})
})
