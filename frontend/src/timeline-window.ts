export type TimelinePeriod='month'|'week'|'day'
export interface TimeWindow {start:number;end:number}
const DAY=86400000
export function periodWindow(period:TimelinePeriod,focus:number):TimeWindow{
 const start=Math.floor(focus),date=new Date(start*DAY)
 if(period==='month')return {start:Date.UTC(date.getUTCFullYear(),date.getUTCMonth(),1)/DAY,end:Date.UTC(date.getUTCFullYear(),date.getUTCMonth()+1,1)/DAY}
 if(period==='week'){const monday=start-(date.getUTCDay()+6)%7;return {start:monday,end:monday+7}}
 return {start,end:start+1}
}
export function shiftPeriod(period:TimelinePeriod,focus:number,direction:number):number{
 if(period!=='month')return focus+direction*(period==='week'?7:1)
 const date=new Date(Math.floor(focus)*DAY)
 return Date.UTC(date.getUTCFullYear(),date.getUTCMonth()+direction,1)/DAY
}
export function clipRange(start:number,end:number,window:TimeWindow){
 const left=Math.max(start,window.start),right=Math.min(end,window.end)
 if(!Number.isFinite(left)||!Number.isFinite(right)||right<=left||window.end<=window.start)return null
 return {left:(left-window.start)/(window.end-window.start)*100,width:(right-left)/(window.end-window.start)*100,before:start<window.start,after:end>window.end}
}
export function moveWindow(window:TimeWindow,delta:number,bounds:TimeWindow):TimeWindow{
 const length=Math.min(window.end-window.start,bounds.end-bounds.start)
 const start=Math.max(bounds.start,Math.min(bounds.end-length,window.start+delta))
 return {start,end:start+length}
}
