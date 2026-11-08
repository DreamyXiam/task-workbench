import type { Row } from './domain'

export function latestProgress(rows:Row[]|undefined):Row|null {
 return (rows??[]).reduce<Row|null>((latest,row)=>{
  const timestamp=Date.parse(String(row.createdAt??''))
  if(!Number.isFinite(timestamp))return latest
  return !latest||timestamp>=Date.parse(String(latest.createdAt))?row:latest
 },null)
}
export function visibleProgress(value:unknown):number|null {
 if(value==null||value==='')return null
 const n=Number(value)
 return Number.isFinite(n)&&n>=0&&n<=100?n:null
}
export function departmentProgress(sub:Row,records:Row[]|undefined):number|null {
 const value=visibleProgress(sub.percent)
 if(value===0&&!(records??[]).some(record=>String(record.subTaskId)===String(sub.id)))return null
 return value
}
