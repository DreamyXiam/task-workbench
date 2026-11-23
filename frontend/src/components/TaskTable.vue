<script setup lang="ts">
import { useRouter } from 'vue-router'
import { TriangleAlert } from 'lucide-vue-next'
import { date,overdue,statuses } from '../domain'
import type { Row } from '../domain'

defineProps<{tasks:Row[]}>()
const router=useRouter()
const priority=(value:string)=>({URGENT:'紧急',HIGH:'高',NORMAL:'普通',LOW:'低'} as Record<string,string>)[value]||'—'
const target=(task:Row)=>'/tasks/'+(task.taskId||task.id)
function warnings(task:Row){
 const result:string[]=[]
 if(overdue(task)){
  const active=!['COMPLETED','ACCEPTED','CANCELLED'].includes(task.status)
  result.push(active&&new Date(task.deadline).getTime()<Date.now()?'已逾期':'曾逾期')
 }
 for(const flag of task.attentionFlags||[])if(flag!=='已逾期'&&!result.includes(flag))result.push(flag)
 return result
}
</script>
<template>
 <div class="task-table-scroll" tabindex="0" aria-label="当前筛选页任务表格">
  <table class="task-table">
   <caption class="sr-only">当前筛选页任务表格</caption>
   <colgroup><col class="title-col"/><col class="state-col"/><col class="dept-col"/><col class="owner-col"/><col class="deadline-col"/><col class="priority-col"/><col class="progress-col"/><col class="attention-col"/></colgroup>
   <thead><tr><th scope="col">任务</th><th scope="col">状态</th><th scope="col">责任部门</th><th scope="col">负责人</th><th scope="col">截止时间</th><th scope="col">优先级</th><th scope="col">最新进展</th><th scope="col">提醒</th></tr></thead>
   <tbody>
<tr v-for="task in tasks" :key="task.kind+':'+task.id" @click="router.push(target(task))">
    <td><router-link class="table-task-link" :to="target(task)" @click.stop>{{ task.title||task.taskTitle||'未命名任务' }}</router-link><small class="table-code">{{ task.code||'未编号' }}</small></td>
    <td><span class="table-state" :class="String(task.status||'').toLowerCase()"><span class="state-dot"/>{{ statuses[task.status]||task.status||'未标注' }}</span></td>
    <td><span>{{ task.hostDeptName||task.deptName||'未指定部门' }}</span><small v-if="task.companyName" class="table-company" :title="task.companyName">{{ task.companyName }}</small></td>
    <td>{{ task.ownerName||task.hostOwnerName||(task.hostVisible===false?'当前岗位不可见':'待分派') }}</td>
    <td>{{ date(task.deadline) }}</td>
    <td><span class="table-priority" :class="{'priority-high':['HIGH','URGENT'].includes(task.priority)}">{{ priority(task.priority) }}</span></td>
    <td><span class="table-progress" :title="task.latestProgress||undefined">{{ task.latestProgress||'暂无进展记录' }}</span><small v-if="task.progressAt">{{ date(task.progressAt) }}</small></td>
    <td><span v-for="warning in warnings(task)" :key="warning" class="table-warning" :class="{'warning-high':warning.includes('逾期')||warning.includes('未见进展')}"><TriangleAlert :size="13" aria-hidden="true"/>{{ warning }}</span><span v-if="!warnings(task).length" class="no-warning">—</span></td>
   </tr>
</tbody>
  </table>
 </div>
</template>
<style scoped>
.task-table-scroll{max-width:100%;overflow-x:auto;border:1px solid var(--line);border-radius:10px;background:var(--surface)}
.task-table-scroll:focus-visible{outline:2px solid var(--brand);outline-offset:2px}
.task-table{width:100%;min-width:1120px;border-collapse:collapse;table-layout:fixed;font-size:12px;color:var(--text)}
.title-col{width:21%}.state-col{width:8%}.dept-col{width:11%}.owner-col{width:11%}.deadline-col{width:15%}.priority-col{width:8%}.progress-col{width:17%}.attention-col{width:9%}
th,td{text-align:left;vertical-align:top;padding:14px 12px;border-bottom:1px solid var(--line);overflow-wrap:anywhere}
th{background:var(--surface-muted);color:var(--muted);font-size:12px;font-weight:600;white-space:nowrap}
tbody tr{cursor:pointer;transition:background-color .15s}tbody tr:hover,tbody tr:focus-within{background:var(--surface-hover)}tbody tr:last-child td{border-bottom:0}
td small{display:block;margin-top:6px;color:var(--muted);font-size:11px;line-height:1.5}
.table-task-link{display:block;color:var(--text);font-size:13px;font-weight:650;line-height:1.5}.table-task-link:hover{text-decoration:underline;color:var(--brand)}
.table-code{font-variant-numeric:tabular-nums}.table-company{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.table-state{display:inline-flex;align-items:center;gap:6px;color:var(--muted);white-space:nowrap;font-weight:600}.state-dot{width:6px;height:6px;border-radius:50%;background:currentColor}.table-state.in_progress,.table-state.rework{color:var(--brand)}.table-state.completed,.table-state.accepted{color:var(--success)}.table-state.pending_acceptance,.table-state.pending_receive,.table-state.submitted{color:var(--warning)}
.table-priority{display:inline-block;padding:2px 7px;border:1px solid var(--line);border-radius:5px;background:var(--surface-muted);color:var(--muted);white-space:nowrap;font-size:11px}.table-priority.priority-high{border-color:color-mix(in srgb,var(--danger) 28%,var(--line));background:color-mix(in srgb,var(--danger) 11%,var(--surface));color:var(--danger)}
.table-progress{display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden;color:var(--muted);line-height:1.6}
.table-warning{display:flex;align-items:flex-start;gap:4px;margin-bottom:6px;color:var(--warning);font-size:11px;font-weight:600}.table-warning svg{flex:none;margin-top:2px}.table-warning.warning-high{color:var(--danger)}.no-warning{color:var(--muted)}
@media(prefers-reduced-motion:reduce){tbody tr{transition:none}}
</style>
