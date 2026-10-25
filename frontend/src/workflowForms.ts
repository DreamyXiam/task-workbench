/** Form-only validation. Server authorization and version checks remain authoritative. */
export function validateTimeWindow(from:string,to?:string):string {
  if(!from||!Number.isFinite(Date.parse(from)))return '请选择有效的开始时间'
  if(to&&(!Number.isFinite(Date.parse(to))||Date.parse(to)<=Date.parse(from)))return '结束时间必须晚于开始时间'
  return ''
}
export function parseCalendarDates(input:string):string[] {
  const dates=input.split(/[\s,，;；]+/).map(x=>x.trim()).filter(Boolean)
  for(const day of dates){if(!/^\d{4}-\d{2}-\d{2}$/.test(day)||!Number.isFinite(Date.parse(day))||new Date(day+'T00:00:00Z').toISOString().slice(0,10)!==day)throw new Error(`日期无效：${day}；请使用YYYY-MM-DD格式`)}
  return [...new Set(dates)].sort()
}
export function validateCandidateWeights(weights:number[]):string {
  if(weights.length!==5||weights.some(w=>!Number.isFinite(w)||w<0||w>100))return '请输入五项0至100的指标权重'
  if(Math.abs(weights.reduce((sum,w)=>sum+w,0)-100)>0.000001)return '五项指标权重合计必须为100'
  return ''
}
export function validateWorkingHours(start:string,end:string):string {
  const time=/^([01]\d|2[0-3]):[0-5]\d$/
  return !time.test(start)||!time.test(end)||end<=start?'工作时段须在同一天内，结束时间晚于开始时间':''
}

export function validateHandoverSelection(items:{subTaskId:string|number;userId:string|number;version:number}[],reason:string):string {
  if(!reason.trim())return '请填写交接原因'
  if(!items.length||items.length>100)return '每次选择1至100项在办任务'
  if(items.some(item=>!item.subTaskId||!item.userId||!Number.isInteger(item.version)||item.version<0))return '请为每项任务选择接收人并刷新当前版本'
  if(new Set(items.map(item=>String(item.subTaskId))).size!==items.length)return '同一子任务不能重复转交'
  return ''
}

export function previousShanghaiMonth(now=new Date()):string {
  const [year,month]=new Intl.DateTimeFormat('sv-SE',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit'}).format(now).split('-').map(Number)
  return new Date(Date.UTC(year,month-2,1)).toISOString().slice(0,7)
}
