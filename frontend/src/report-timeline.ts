import type {Row} from './domain'
import {visibleProgress} from './detail-progress'
import {dayNumber} from './task-gantt'
export const DAY_MS=86400000
export interface ReportEvent {id:string;at:number;createdAt:string;content:string;percent:number|null;department:string;author:string;source:string}
export interface ReportSegment {start:number;end:number;events:ReportEvent[]}
export function timePoint(value:unknown):number|null{
 if(typeof value!=='string'||!value.trim())return null
 if(/^\d{4}-\d{2}-\d{2}$/.test(value))return dayNumber(value)
 const stamp=Date.parse(value)
 return Number.isFinite(stamp)?stamp/DAY_MS+8/24:null
}
export const pointISO=(point:number)=>new Date((point-8/24)*DAY_MS).toISOString()
export function reportEvents(detail:Row):{events:ReportEvent[];invalid:number}{
 const events:ReportEvent[]=[];let invalid=0
 for(const [index,row] of (detail.progress??[] as Row[]).entries()){
  const at=timePoint(row.createdAt)
  if(at===null){invalid++;continue}
  const sub=detail.subTasks?.find((s:Row)=>String(s.id)===String(row.subTaskId)||row.deptId!=null&&String(s.deptId)===String(row.deptId))
  events.push({id:'progress:'+(row.id??index),at,createdAt:row.createdAt,content:row.note||'未填写汇报说明',percent:visibleProgress(row.percent),department:row.deptName||sub?.deptName||'未注明部门',author:row.authorName||row.userName||'',source:'部门进度'})
 }
 for(const [index,row] of (detail.keyWorkHistory??[] as Row[]).entries()){
  if(row.status!=='FILL')continue
  const before=row.before?.values??{},after=row.after?.values??{}
  const fields:Row[]=row.after?.fields??[]
  const changes=fields.filter(f=>JSON.stringify(before[f.id])!==JSON.stringify(after[f.id])).map(f=>`${f.name}：${after[f.id]==null||after[f.id]===''?'（已清空）':typeof after[f.id]==='object'?JSON.stringify(after[f.id]):String(after[f.id])}`)
  if(!changes.length)continue
  const at=timePoint(row.createdAt)
  if(at===null){invalid++;continue}
  events.push({id:'fill:'+(row.id??index),at,createdAt:row.createdAt,content:changes.join('\n'),percent:null,department:row.deptName||'',author:row.actorName||'',source:'重点工作填报'})
 }
 return {events:events.sort((a,b)=>a.at-b.at||a.id.localeCompare(b.id,undefined,{numeric:true})),invalid}
}
export function reportSegments(task:Row,events:ReportEvent[]):ReportSegment[]{
 const start=timePoint(task.createdAt),deadline=dayNumber(task.deadline)
 if(start===null||deadline===null||deadline<Math.floor(start))return []
 const visible=events.filter(e=>e.at>=start)
 const end=Math.max(deadline+1,...visible.map(e=>Math.floor(e.at)+1))
 const groups=new Map<number,ReportEvent[]>()
 for(const event of visible){const entries=groups.get(event.at)??[];entries.push(event);groups.set(event.at,entries)}
 const boundaries=Array.from(new Set([start,...groups.keys()])).sort((a,b)=>a-b)
 return boundaries.map((point,i)=>({start:point,end:boundaries[i+1]??end,events:groups.get(point)??[]})).filter(s=>s.end>s.start)
}
export const activeSegment=(segments:ReportSegment[],point:number)=>segments.find(s=>point>=s.start&&point<s.end)
