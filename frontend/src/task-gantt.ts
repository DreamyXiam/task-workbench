import type {Row} from './domain'
const DAY=86400000
const formatter=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'})
export function dayNumber(value:unknown):number|null{
 if(typeof value!=='string'||!value.trim())return null
 if(/^\d{4}-\d{2}-\d{2}$/.test(value)){
  const stamp=Date.parse(value+'T00:00:00Z')
  return Number.isFinite(stamp)&&new Date(stamp).toISOString().slice(0,10)===value?stamp/DAY:null
 }
 const stamp=Date.parse(value)
 if(!Number.isFinite(stamp))return null
 const parts=formatter.formatToParts(new Date(stamp)),part=(name:string)=>parts.find(p=>p.type===name)?.value
 return Date.UTC(Number(part('year')),Number(part('month'))-1,Number(part('day')))/DAY
}
export const dayLabel=(day:number)=>new Date(day*DAY).toISOString().slice(0,10)
export function ganttLayout(tasks:Row[]){
 const ranges=tasks.map(task=>{
  const start=dayNumber(task.createdAt),end=dayNumber(task.deadline)
  return {task,start,end,issue:start===null||end===null?'缺少创建或截止时间':end<start?'截止时间早于创建时间':''}
 })
 const valid=ranges.filter(r=>!r.issue),today=dayNumber(new Date().toISOString())!
 const start=valid.length?Math.min(...valid.map(r=>r.start!))-1:today-3
 const end=valid.length?Math.max(...valid.map(r=>r.end!))+2:today+4
 const span=end-start
 const ticks=Array.from(new Set(Array.from({length:Math.min(7,span)+1},(_,i)=>start+Math.round(i*span/Math.min(7,span)))))
 return {start,end,today,ticks,rows:ranges.map(r=>({...r,left:r.issue?0:(r.start!-start)/span*100,width:r.issue?0:(r.end!-r.start!+1)/span*100}))}
}
