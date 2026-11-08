<script setup lang="ts">
import {computed} from 'vue'
import type {Row} from '../domain'
import {date,overdue,statuses} from '../domain'
import {visibleProgress} from '../detail-progress'
import {dayLabel,ganttLayout} from '../task-gantt'
const props=defineProps<{tasks:Row[]}>()
const chart=computed(()=>ganttLayout(props.tasks))
const position=(day:number)=>(day-chart.value.start)/(chart.value.end-chart.value.start)*100+'%'
const done=(task:Row)=>['COMPLETED','ACCEPTED'].includes(task.status)
function details(task:Row){return `${task.title||task.taskTitle}\n创建：${date(task.createdAt)}\n截止：${date(task.deadline)}\n${statuses[task.status]||task.status}\n最新进展：${task.latestProgress||'暂无进展记录'}${visibleProgress(task.reportedPercent)!==null?' · '+task.reportedPercent+'%':''}`}
</script>
<template>
 <section class="gantt-card" aria-label="任务甘特图">
  <header class="gantt-heading"><div><h2>任务时间线</h2><p>当前页 {{ tasks.length }} 项任务 · 时间条表示创建至截止日期（北京时间）</p></div><div class="legend"><span><i class="active"/>进行中 / 待处理</span><span><i class="finished"/>已完成 / 已验收</span><span><i class="late"/>逾期</span></div></header>
  <p class="gantt-hint">点击任务或时间条查看详情 · 横向滚动查看完整时间线</p>
  <div class="gantt-scroll" tabindex="0" role="region" aria-label="可横向滚动的任务时间线">
   <div class="gantt-content">
    <div class="gantt-axis"><div class="task-label">任务与最新进展</div><div class="axis-track"><span v-for="tick in chart.ticks" :key="tick" :style="{left:position(tick)}">{{ dayLabel(tick) }}</span></div></div>
    <div v-for="row in chart.rows" :key="String(row.task.kind)+':'+row.task.id" class="gantt-row">
     <div class="task-label"><router-link :to="'/tasks/'+(row.task.taskId||row.task.id)" class="task-title">{{ row.task.title||row.task.taskTitle }}</router-link><div class="task-state"><span>{{ statuses[row.task.status]||row.task.status }}</span><span>{{ row.task.hostDeptName||row.task.deptName||'未指定部门' }}</span><b v-if="visibleProgress(row.task.reportedPercent)!==null">{{ row.task.reportedPercent }}%</b></div><p :title="row.task.latestProgress">{{ row.task.latestProgress||'暂无进展记录' }}</p><small v-if="row.task.progressAt">更新于 {{ date(row.task.progressAt) }}</small></div>
     <div class="row-track">
      <span v-for="tick in chart.ticks" :key="tick" class="grid-line" :style="{left:position(tick)}"/><span v-if="chart.today>=chart.start&&chart.today<chart.end" class="today-line" :style="{left:position(chart.today)}" aria-hidden="true"/>
      <p v-if="row.issue" class="date-issue">{{ row.issue }}</p>
      <router-link v-else :to="'/tasks/'+(row.task.taskId||row.task.id)" class="time-bar" :class="{finished:done(row.task),late:overdue(row.task),inactive:['CANCELLED','WITHDRAWN'].includes(row.task.status)}" :style="{left:row.left+'%',width:row.width+'%'}" :title="details(row.task)" :aria-label="details(row.task)"><span>{{ statuses[row.task.status]||row.task.status }}</span></router-link>
     </div>
    </div>
   </div>
  </div><footer>蓝色竖线为今天 · 时间条长度表示日期跨度，填报进度单独显示为百分比。</footer>
 </section>
</template>
<style scoped>
.gantt-card{min-width:0;background:var(--surface);border:1px solid var(--line);border-radius:12px;overflow:hidden}.gantt-heading{display:flex;justify-content:space-between;gap:18px;padding:22px 22px 8px;flex-wrap:wrap}.gantt-heading h2{font-size:17px;margin:0 0 8px}.gantt-heading p,.gantt-hint,.legend,footer{font-size:12px;color:var(--muted)}.gantt-heading p{margin:0}.legend{display:flex;gap:14px;flex-wrap:wrap;align-items:center}.legend span{display:flex;align-items:center;gap:5px}.legend i{width:8px;height:8px;border-radius:3px;background:var(--el-color-primary)}.legend .finished{background:#16825d}.legend .late{background:#b93843}.gantt-hint{padding:0 22px;margin:8px 0 16px}.gantt-scroll{overflow:auto;max-height:650px;isolation:isolate}.gantt-scroll:focus-visible{outline:2px solid var(--brand);outline-offset:-2px}.gantt-content{min-width:1000px}.gantt-axis,.gantt-row{display:grid;grid-template-columns:290px minmax(710px,1fr)}.gantt-axis{position:sticky;top:0;z-index:4;background:var(--surface-muted);border-block:1px solid var(--line);height:48px;font-size:11px;color:var(--muted)}.task-label{position:sticky;left:0;z-index:2;background:var(--surface);padding:16px 20px;border-right:1px solid var(--line);min-width:0}.gantt-axis .task-label{background:var(--surface-muted);z-index:5}.axis-track,.row-track{position:relative;margin:0 46px}.axis-track span{position:absolute;top:17px;transform:translateX(-50%);white-space:nowrap;font-variant-numeric:tabular-nums}.gantt-row{border-bottom:1px solid var(--line);min-height:128px}.gantt-row:hover .task-label,.gantt-row:hover{background:var(--surface-muted)}.task-title{display:block;font-size:13px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.task-state{display:flex;gap:8px;margin-top:6px;font-size:11px;color:var(--muted);flex-wrap:wrap}.task-state b{color:var(--brand)}.task-label p{font-size:12px;margin:8px 0 4px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--muted)}.task-label small{font-size:10px;color:var(--muted)}.grid-line{position:absolute;top:0;bottom:0;border-left:1px dashed var(--line);pointer-events:none}.today-line{position:absolute;inset-block:0;border-left:2px solid var(--brand);opacity:.5;pointer-events:none}.time-bar{position:absolute;top:44px;height:32px;min-width:6px;border-radius:6px;background:var(--el-color-primary);color:#fff;display:flex;align-items:center;overflow:hidden;box-shadow:0 2px 4px #0001}.time-bar span{font-size:11px;padding:0 10px;white-space:nowrap}.time-bar.finished{background:#16825d}.time-bar.late{background:#b93843}.time-bar.inactive{background:#71717a}.time-bar:focus-visible,.task-title:focus-visible{outline:2px solid var(--text);outline-offset:3px}.date-issue{font-size:12px;color:var(--muted);margin-top:50px}.gantt-card footer{padding:14px 22px;line-height:1.7}@media(max-width:767px){.gantt-heading{padding:18px 16px 8px}.gantt-hint{padding-inline:16px}.gantt-axis,.gantt-row{grid-template-columns:190px minmax(710px,1fr)}.gantt-content{min-width:900px}.task-label{padding:14px 12px}.gantt-card footer{padding:12px 16px}}
</style>
