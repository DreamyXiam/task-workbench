<script setup lang="ts">
import {computed,nextTick,onBeforeUnmount,ref,watch} from 'vue'
import type {Row} from '../domain'
import {date,overdue,statuses} from '../domain'
import {api,errorText} from '../api'
import {useAuth} from '../stores/auth'
import {visibleProgress} from '../detail-progress'
import {dayLabel,ganttLayout} from '../task-gantt'
import {activeSegment,pointISO,reportEvents,reportSegments,timePoint} from '../report-timeline'
import type {ReportSegment} from '../report-timeline'
const props=defineProps<{tasks:Row[]}>(),auth=useAuth()
const histories=ref<Record<string,{signature:string;detail:Row}>>({}),failures=ref<Record<string,string>>({}),loading=ref(false),zoom=ref(1),scroll=ref<HTMLElement>(),axis=ref<HTMLElement>()
const cursor=ref(timePoint(new Date().toISOString())!),selected=ref('')
const key=(task:Row)=>String(task.taskId||task.id)
const context=computed(()=>JSON.stringify([auth.me?.id,auth.me?.role,auth.me?.companyId,auth.me?.deptId,auth.me?.delegation?.id,auth.me?.permissions]))
const signature=(task:Row)=>JSON.stringify([task.id,task.version,task.progressAt,task.latestProgress,context.value])
let generation=0,alive=true,previousContext='',touched=false
async function loadHistory(force=false){
 const run=++generation
 if(previousContext!==context.value){histories.value={};selected.value='';previousContext=context.value}
 const next={...histories.value},errors:Record<string,string>={}
 const ids=new Set(props.tasks.map(key))
 for(const id of Object.keys(next))if(!ids.has(id))delete next[id]
 histories.value=next;failures.value={};loading.value=true
 const queue=props.tasks.filter(task=>force||next[key(task)]?.signature!==signature(task))
 await Promise.all(Array.from({length:Math.min(4,queue.length)},async()=>{
  while(queue.length&&alive&&run===generation){
   const task=queue.shift()!,id=key(task),stamp=signature(task)
   try{const detail=await api.get<Row>('/tasks/'+encodeURIComponent(id));if(run===generation&&alive)next[id]={signature:stamp,detail}}
   catch(error){delete next[id];errors[id]=errorText(error)}
  }
 }))
 if(alive&&run===generation){histories.value={...next};failures.value=errors;loading.value=false}
}
watch(()=>JSON.stringify(props.tasks.map(signature)),()=>{void loadHistory()}, {immediate:true})
onBeforeUnmount(()=>{alive=false;generation++})
const history=(task:Row)=>histories.value[key(task)]?.detail
const rows=computed(()=>props.tasks.map(task=>{
 const detail=history(task),reports=reportEvents(detail??{})
 return {task,events:reports.events,invalid:reports.invalid,segments:reportSegments(task,reports.events),loaded:!!detail}
}))
const chart=computed(()=>ganttLayout(props.tasks,rows.value.flatMap(row=>row.events.map(event=>event.at))))
const ticks=computed(()=>{
 const step=Math.max(1,Math.ceil((chart.value.end-chart.value.start)*24/(8*zoom.value))),result:number[]=[]
 for(let hour=Math.ceil(chart.value.start*24/step)*step;hour<=chart.value.end*24&&result.length<260;hour+=step)result.push(hour/24)
 return result
})
const tickLabel=(point:number)=>zoom.value>4?new Date(point*86400000).toISOString().slice(5,16).replace('T',' '):dayLabel(point)
const position=(point:number)=>(point-chart.value.start)/(chart.value.end-chart.value.start)*100+'%'
const span=(start:number,end:number)=>(end-start)/(chart.value.end-chart.value.start)*100+'%'
const clamp=(point:number)=>Math.min(chart.value.end-1/86400,Math.max(chart.value.start,point))
watch(()=>[chart.value.start,chart.value.end],()=>{cursor.value=clamp(touched?cursor.value:timePoint(new Date().toISOString())!)},{immediate:true})
const current=computed(()=>rows.value.find(row=>key(row.task)===selected.value)??rows.value[0])
const currentSegment=computed(()=>current.value?activeSegment(current.value.segments,cursor.value):undefined)
const done=(task:Row)=>['COMPLETED','ACCEPTED'].includes(task.status)
const cursorLabel=computed(()=>date(pointISO(cursor.value)))
function setCursor(point:number){touched=true;cursor.value=clamp(point)}
function choose(task:Row,segment:ReportSegment){selected.value=key(task);setCursor(segment.start)}
function scrub(event:PointerEvent){
 if(event.button!==0)return
 const target=event.currentTarget as HTMLElement
 const update=(e:PointerEvent)=>{const box=target.getBoundingClientRect();setCursor(chart.value.start+(e.clientX-box.left)/box.width*(chart.value.end-chart.value.start))}
 target.setPointerCapture(event.pointerId);update(event)
 target.onpointermove=update
 const stop=()=>{target.onpointermove=null;target.onpointerup=null;target.onpointercancel=null;target.onlostpointercapture=null}
 target.onpointerup=stop;target.onpointercancel=stop;target.onlostpointercapture=stop
 event.preventDefault()
}
function scrubKey(event:KeyboardEvent){
 const step=event.shiftKey?1:1/24
 const point=({ArrowLeft:cursor.value-step,ArrowRight:cursor.value+step,Home:chart.value.start,End:chart.value.end} as Record<string,number>)[event.key]
 if(point!==undefined){event.preventDefault();setCursor(point)}
}
function pan(event:PointerEvent){
 if(event.button!==0||event.pointerType!=='mouse'||!scroll.value)return
 const target=event.currentTarget as HTMLElement,origin=event.clientX,left=scroll.value.scrollLeft,viewport=scroll.value
 target.setPointerCapture(event.pointerId)
 target.onpointermove=e=>{viewport.scrollLeft=left+origin-e.clientX}
 const stop=()=>{target.onpointermove=null;target.onpointerup=null;target.onpointercancel=null;target.onlostpointercapture=null}
 target.onpointerup=stop;target.onpointercancel=stop;target.onlostpointercapture=stop
}
function revealCursor(){if(axis.value&&scroll.value)scroll.value.scrollLeft=axis.value.offsetLeft+(cursor.value-chart.value.start)/(chart.value.end-chart.value.start)*axis.value.clientWidth-scroll.value.clientWidth/2}
function today(){setCursor(timePoint(new Date().toISOString())!);revealCursor()}
async function changeZoom(direction:number){zoom.value=Math.max(1,Math.min(32,zoom.value*(direction>0?2:.5)));await nextTick();revealCursor()}
</script>
<template>
 <section class="gantt-card" aria-label="任务甘特图">
  <header class="gantt-heading"><div><h2>任务时间线</h2><p>当前页 {{ tasks.length }} 项任务 · 北京时间</p></div><div class="legend"><span><i/>任务期限</span><span><i class="report"/>汇报片段</span><span><i class="playhead-color"/>浏览指针</span></div></header>
  <div class="timeline-toolbar"><div class="cursor-readout"><span>浏览时间</span><strong>{{ cursorLabel }}</strong></div><div class="timeline-controls"><el-button size="small" @click="today">今天</el-button><el-button size="small" aria-label="缩小时间轴" :disabled="zoom<=1" @click="changeZoom(-1)">−</el-button><span>{{ zoom }}×</span><el-button size="small" aria-label="放大时间轴" :disabled="zoom>=32" @click="changeZoom(1)">＋</el-button></div></div>
  <p class="gantt-hint">拖动上方刻度浏览时间 · 拖动轨道空白处平移 · 点击汇报片段查看内容</p>
  <div v-if="Object.keys(failures).length" class="history-error" role="alert">{{ Object.keys(failures).length }} 项任务的汇报记录未能加载。<el-button size="small" @click="loadHistory(true)">重新加载</el-button></div>
  <div ref="scroll" class="gantt-scroll" tabindex="0" role="region" aria-label="可横向滚动的任务时间线">
   <div class="gantt-content" :style="{'--zoom':zoom}">
    <div class="gantt-axis"><div class="task-label">任务 / 最新进展<small>{{ loading?'正在加载汇报记录…':'任务期限 · 汇报记录' }}</small></div><div ref="axis" class="axis-track" role="slider" tabindex="0" aria-label="时间指针" :aria-valuemin="chart.start" :aria-valuemax="chart.end" :aria-valuenow="cursor" :aria-valuetext="cursorLabel" @pointerdown="scrub" @keydown="scrubKey"><span v-for="tick in ticks" :key="tick" class="axis-tick" :style="{left:position(tick)}">{{ tickLabel(tick) }}</span><span class="playhead-knob" :style="{left:position(cursor)}" aria-hidden="true"/></div></div>
    <div v-for="(row,index) in rows" :key="key(row.task)" class="gantt-row" :class="{'selected-row':current&&key(current.task)===key(row.task)}">
     <div class="task-label"><router-link :to="'/tasks/'+key(row.task)" class="task-title">{{ row.task.title||row.task.taskTitle }}</router-link><div class="task-state"><span>{{ statuses[row.task.status]||row.task.status }}</span><span>{{ row.task.hostDeptName||row.task.deptName||'未指定部门' }}</span><b v-if="visibleProgress(row.task.reportedPercent)!==null">{{ row.task.reportedPercent }}%</b></div><p :title="row.task.latestProgress">{{ row.task.latestProgress||'暂无进展记录' }}</p><small>{{ row.loaded?row.events.length+' 条汇报':failures[key(row.task)]?'记录加载失败':'正在加载汇报…' }}<template v-if="row.invalid"> · {{ row.invalid }} 条时间异常</template></small></div>
     <div class="row-track" @pointerdown="pan">
      <span v-for="tick in ticks" :key="tick" class="grid-line" :style="{left:position(tick)}"/><span v-if="chart.today>=chart.start&&chart.today<chart.end" class="today-line" :style="{left:position(chart.today)}" aria-hidden="true"/>
      <p v-if="chart.rows[index].issue" class="date-issue">{{ chart.rows[index].issue }}</p>
      <template v-else>
       <router-link :to="'/tasks/'+key(row.task)" class="time-bar" :class="{finished:done(row.task),late:overdue(row.task),inactive:['CANCELLED','WITHDRAWN'].includes(row.task.status)}" :style="{left:chart.rows[index].left+'%',width:chart.rows[index].width+'%'}" :title="'查看任务详情 · 截止 '+date(row.task.deadline)" @pointerdown.stop><span>{{ statuses[row.task.status]||row.task.status }}</span></router-link>
       <button v-for="segment in row.loaded?row.segments:[]" :key="segment.start" type="button" class="report-clip" :class="{unreported:!segment.events.length,'clip-selected':key(current?.task??{})===key(row.task)&&currentSegment?.start===segment.start}" :style="{left:position(segment.start),width:span(segment.start,segment.end)}" :title="segment.events.map(e=>date(e.createdAt)+' · '+e.content).join('\n')||'此区间尚无汇报'" :aria-label="(row.task.title||row.task.taskTitle)+' · '+(segment.events.length?date(segment.events[0].createdAt)+' '+segment.events.map(e=>e.content).join('；'):'尚无汇报')" @pointerdown.stop @click="choose(row.task,segment)"><span>{{ segment.events[0]?.content||'尚无汇报' }}</span><b v-if="segment.events.length>1">{{ segment.events.length }} 条</b></button>
      </template>
      <span class="playhead-line" :style="{left:position(cursor)}" aria-hidden="true"/>
     </div>
    </div>
   </div>
  </div>
  <section v-if="current" class="report-inspector" aria-label="指针处汇报内容">
   <div class="inspector-heading"><div><span class="inspector-caption">指针处的汇报</span><h3>{{ current.task.title||current.task.taskTitle }}</h3></div><router-link :to="'/tasks/'+key(current.task)">查看任务详情 ↗</router-link></div>
   <p v-if="failures[key(current.task)]" role="alert">{{ failures[key(current.task)] }}</p><p v-else-if="!current.loaded" class="muted">正在加载汇报记录…</p>
   <template v-else><p class="interval-label">浏览时间 {{ cursorLabel }}<template v-if="currentSegment"> · 片段 {{ date(pointISO(currentSegment.start)) }} — {{ date(pointISO(currentSegment.end)) }}</template></p><template v-if="currentSegment?.events.length"><article v-for="event in currentSegment.events" :key="event.id" class="report-entry"><div><b>{{ event.source }}</b><span>{{ event.department }}<template v-if="event.author"> · {{ event.author }}</template></span><strong v-if="event.percent!==null">{{ event.percent }}%</strong><time>{{ date(event.createdAt) }}</time></div><p>{{ event.content }}</p></article></template><p v-else class="muted">{{ currentSegment?'此时间段尚无汇报。':'指针位于该任务的时间范围之外。' }}</p></template>
  </section>
  <footer>上轨为创建至截止日期，下轨按实际汇报时间分段；最后一次汇报延续展示至期限末或最后汇报当日，不代表期间持续更新。蓝线为今天，橙线为浏览时间。拖动仅用于浏览。刻度获焦后用 ← / → 按小时微调，Shift 按天调整。</footer>
 </section>
</template>
<style scoped>
.gantt-card{--playhead:#c2610c;min-width:0;background:var(--surface);border:1px solid var(--line);border-radius:12px;overflow:hidden}.gantt-heading{display:flex;justify-content:space-between;gap:18px;padding:22px 22px 12px;flex-wrap:wrap}.gantt-heading h2{font-size:17px;margin:0 0 8px}.gantt-heading p,.gantt-hint,.legend,footer{font-size:12px;color:var(--muted)}.gantt-heading p{margin:0}.legend{display:flex;gap:14px;flex-wrap:wrap;align-items:center}.legend span{display:flex;align-items:center;gap:5px}.legend i{width:8px;height:8px;border-radius:3px;background:var(--el-color-primary)}.legend .report{background:#16825d}.legend .playhead-color{background:var(--playhead)}.timeline-toolbar{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;margin:0 22px;padding:12px 14px;background:var(--surface-muted);border:1px solid var(--line);border-radius:8px}.cursor-readout,.timeline-controls{display:flex;align-items:center;gap:12px;font-size:12px}.cursor-readout span{color:var(--muted)}.cursor-readout strong{font-variant-numeric:tabular-nums;font-weight:600}.timeline-controls .el-button+.el-button{margin-left:0}.gantt-hint{padding:0 22px;margin:12px 0 16px}.history-error{padding:12px 22px;color:var(--danger);font-size:12px}.gantt-scroll{scroll-padding-left:290px;scroll-padding-block:80px;overflow:auto;max-height:650px;isolation:isolate}.gantt-scroll:focus-visible{outline:2px solid var(--brand);outline-offset:-2px}.gantt-content{min-width:calc(1000px * var(--zoom))}.gantt-axis,.gantt-row{display:grid;grid-template-columns:290px minmax(710px,1fr)}.gantt-axis{position:sticky;top:0;z-index:4;background:var(--surface-muted);border-block:1px solid var(--line);height:64px;font-size:11px;color:var(--muted)}.task-label{position:sticky;left:0;z-index:2;background:var(--surface);padding:16px 20px;border-right:1px solid var(--line);min-width:0}.gantt-axis .task-label{background:var(--surface-muted);z-index:5}.gantt-axis small{display:block;margin-top:6px}.axis-track,.row-track{position:relative;margin:0 46px}.axis-track{cursor:ew-resize;touch-action:none;user-select:none;outline-offset:-2px}.axis-track:focus-visible{outline:2px solid var(--brand)}.axis-tick{position:absolute;top:15px;transform:translateX(-50%);white-space:nowrap;font-variant-numeric:tabular-nums;pointer-events:none}.playhead-knob{position:absolute;bottom:0;transform:translateX(-50%);height:16px;width:12px;background:var(--playhead);clip-path:polygon(0 0,100% 0,100% 65%,50% 100%,0 65%);pointer-events:none}.gantt-row{border-bottom:1px solid var(--line);min-height:132px}.gantt-row:hover .task-label,.gantt-row:hover{background:var(--surface-muted)}.selected-row .task-label{box-shadow:inset 3px 0 var(--playhead)}.task-title{display:block;font-size:13px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.task-state{display:flex;gap:8px;margin-top:6px;font-size:11px;color:var(--muted);flex-wrap:wrap}.task-state b{color:var(--brand)}.task-label p{font-size:12px;margin:8px 0 4px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--muted)}.task-label small{font-size:10px;color:var(--muted)}.row-track{cursor:grab}.row-track:active{cursor:grabbing}.grid-line{position:absolute;inset-block:0;border-left:1px dashed var(--line);pointer-events:none}.today-line{position:absolute;inset-block:0;border-left:1px solid var(--brand);opacity:.45;pointer-events:none}.playhead-line{position:absolute;inset-block:0;border-left:2px solid var(--playhead);pointer-events:none;z-index:1}.time-bar{position:absolute;top:22px;height:20px;min-width:4px;border-radius:4px;background:var(--el-color-primary);color:#fff;display:flex;align-items:center;overflow:hidden}.time-bar span{font-size:10px;padding:0 8px;white-space:nowrap}.time-bar.finished{background:#16825d}.time-bar.late{background:#b93843}.time-bar.inactive{background:#71717a}.time-bar:focus-visible,.task-title:focus-visible,.report-clip:focus-visible{outline:2px solid var(--brand);outline-offset:3px;z-index:2}.report-clip{scroll-margin-block:80px;position:absolute;top:60px;height:40px;border:1px solid #16825d;border-left:3px solid #126c4d;border-radius:4px;background:#e8f5ef;color:#174f3c;min-width:3px;overflow:hidden;text-align:left;display:flex;align-items:center;gap:6px;padding:0;cursor:pointer;font:inherit}.report-clip span{padding:0 8px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:11px}.report-clip b{font-size:10px;white-space:nowrap}.report-clip.unreported{background:var(--surface-muted);border:1px dashed var(--line);color:var(--muted)}.report-clip.clip-selected{outline:2px solid var(--playhead);outline-offset:1px;z-index:1}.date-issue{font-size:12px;color:var(--muted);margin-top:50px}.report-inspector{padding:20px 22px;border-top:1px solid var(--line);background:var(--surface-muted)}.inspector-heading{display:flex;justify-content:space-between;align-items:center;gap:14px;flex-wrap:wrap}.inspector-caption{font-size:11px;color:var(--playhead)}.inspector-heading h3{font-size:14px;margin:6px 0;overflow-wrap:anywhere}.inspector-heading a{font-size:12px;color:var(--brand)}.interval-label{font-size:11px;color:var(--muted);line-height:1.8}.report-entry{padding:14px;margin-top:10px;border:1px solid var(--line);background:var(--surface);border-radius:8px}.report-entry>div{display:flex;align-items:center;gap:10px;flex-wrap:wrap;font-size:11px}.report-entry span,.report-entry time{color:var(--muted)}.report-entry strong{color:var(--brand)}.report-entry p{font-size:13px;line-height:1.8;white-space:pre-wrap;overflow-wrap:anywhere;margin:10px 0 0}.gantt-card footer{padding:14px 22px;line-height:1.8}:global(:root[data-theme='dark'] .gantt-card){--playhead:#ffb962}:global(:root[data-theme='dark'] .report-clip:not(.unreported)){background:#12372a;color:#b7f0d4;border-color:#2a996d}@media(max-width:767px){.gantt-heading{padding:18px 16px 12px}.timeline-toolbar{margin-inline:16px}.gantt-hint{padding-inline:16px}.gantt-axis,.gantt-row{grid-template-columns:140px minmax(710px,1fr)}.gantt-content{min-width:calc(850px * var(--zoom))}.gantt-scroll{scroll-padding-left:140px}.task-label{padding:14px 12px}.gantt-card footer,.report-inspector{padding:12px 16px}.cursor-readout{flex-wrap:wrap;gap:6px}}
</style>
