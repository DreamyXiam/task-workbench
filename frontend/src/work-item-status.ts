import type {Row} from './domain'
export type WorkItemStatus='IN_PROGRESS'|'COMPLETED'
export function workItemStatus(item:Row):WorkItemStatus{
 return item.status==='COMPLETED'||!item.status&&Number(item.percent)===100?'COMPLETED':'IN_PROGRESS'
}
// Existing endpoints retain their numeric wire format; the UI edits workflow state.
export function workItemWirePercent(item:Row,status:WorkItemStatus):number{
 if(status==='COMPLETED')return 100
 const old=Number(item.percent)
 return Number.isFinite(old)&&old>=0&&old<100?old:0
}
