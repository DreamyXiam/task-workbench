<script setup lang="ts">
import { computed } from 'vue'
import { allowed,date } from '../domain'
import type { Row } from '../domain'
import { latestProgress } from '../detail-progress'
import { summaryValue } from '../key-work'
import { useAuth } from '../stores/auth'
import StatusBadge from './StatusBadge.vue'

const props=defineProps<{task:Row}>(),emit=defineEmits<{ progress:[sub:Row];report:[];history:[] }>(),auth=useAuth()
const latest=computed(()=>latestProgress(props.task.progress))
const latestDepartment=computed(()=>props.task.subTasks?.find((sub:Row)=>String(sub.deptId)===String(latest.value?.deptId))?.deptName)
const hasReporting=computed(()=>!!props.task.keyWorkData?.enabled&&props.task.keyWorkData?.fields?.some((field:Row)=>field.id==='progress'&&field.name==='推进情况'))
const reporting=computed(()=>{const value=summaryValue(props.task.keyWorkData,'progress');return value==='—'?'':String(value)})
const workItems=computed<Row[]>(()=>props.task.workItems??[])
const completed=computed(()=>workItems.value.filter(item=>item.status==='COMPLETED').length)
const mayUpdate=computed(()=>!auth.me?.permissions||auth.me.permissions.includes('TASK_UPDATE'))
</script>
<template>
<section class="overview panel" aria-labelledby="task-progress-heading">
 <div class="overview-head"><div><p class="eyebrow">TASK PROGRESS</p><h2 id="task-progress-heading">任务进展</h2><p class="muted">各部门分别填报，点击进度记录查看历史。</p></div><el-button text type="primary" @click="emit('history')">查看进度记录 →</el-button></div>
 <div v-if="task.subTasks?.length" class="department-grid"><article v-for="sub in task.subTasks" :key="sub.id" class="department-progress"><div class="department-top"><div><span class="role-label">{{ sub.isHost?'主办':'协办' }}</span><strong>{{ sub.deptName||'部门名称不可用' }}</strong></div><StatusBadge :status="sub.status"/></div><p class="person">负责人：{{ sub.ownerName||'待分派' }}</p><el-button v-if="allowed(sub,'progress')&&mayUpdate" class="update-button" size="small" type="primary" plain @click="emit('progress',sub)">更新部门进度</el-button></article></div>
 <p v-else class="empty-note">暂无可查看的部门执行进度。</p>
 <div class="progress-details"><div class="detail-block"><div class="detail-label">最新执行记录 <span v-if="latestDepartment">· {{ latestDepartment }}</span></div><p v-if="latest" class="detail-text">{{ latest.note }}</p><p v-else class="empty-note">尚未填写执行进展</p><small v-if="latest">{{ date(latest.createdAt) }}</small></div><div v-if="hasReporting" class="detail-block"><div class="detail-label">重点工作 · 推进情况</div><p v-if="reporting" class="detail-text">{{ reporting }}</p><p v-else class="empty-note">尚未填写推进情况</p><el-button v-if="task.canFillFields&&mayUpdate&&!['COMPLETED','CANCELLED'].includes(task.status)&&task.keyWorkData?.fields?.length" text type="primary" @click="emit('report')">填写工作进展 →</el-button></div><div v-if="workItems.length" class="detail-block"><div class="detail-label">可见子任务</div><p class="detail-text"><strong>{{ completed }} / {{ workItems.length }}</strong> 已完成</p><small>按当前岗位可见范围统计</small></div></div>
</section>
</template>
<style scoped>
.overview{margin-bottom:20px}.overview-head{display:flex;justify-content:space-between;align-items:flex-start;gap:16px;margin-bottom:16px}.overview-head h2{margin:0 0 3px}.overview-head .muted{font-size:12px}.overview-head .el-button{flex-shrink:0}.department-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(220px,100%),1fr));gap:12px}.department-progress{padding:15px;border:1px solid #dfe8f1;border-radius:8px;background:#f8fbfe;min-width:0}.department-top{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}.department-top strong{overflow-wrap:anywhere}.role-label{font-size:11px;color:var(--brand);background:#e4f0fa;padding:3px 7px;border-radius:4px;margin-right:8px}.person{font-size:12px;color:var(--muted);margin:10px 0}.progress-heading{display:flex;justify-content:space-between;gap:12px;font-size:12px;margin:10px 0 6px}.progress-heading strong{color:var(--brand);font-size:14px}.update-button{margin-top:12px}.progress-details{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(200px,100%),1fr));gap:12px;margin-top:16px}.detail-block{min-width:0;padding:12px 14px;border-left:3px solid #d5e5f4;background:#f8fafc;border-radius:0 6px 6px 0}.detail-label{font-size:12px;font-weight:600;color:#536a83}.detail-text{margin:6px 0;white-space:pre-wrap;overflow-wrap:anywhere}.detail-block small,.empty-note{font-size:12px;color:var(--muted)}.empty-note{margin:8px 0}.detail-block .el-button{padding-left:0;height:auto}.detail-block .el-button:focus-visible{outline:2px solid var(--brand)}@media(max-width:767px){.overview-head{flex-wrap:wrap}.overview-head .el-button{padding-left:0}.progress-details{grid-template-columns:1fr}}
</style>
