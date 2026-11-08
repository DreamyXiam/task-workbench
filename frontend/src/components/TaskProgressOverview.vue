<script setup lang="ts">
import {computed} from 'vue'
import {date} from '../domain'
import type {Row} from '../domain'
import {reportEvents} from '../report-timeline'
const props=defineProps<{task:Row}>(),emit=defineEmits<{history:[]}>()
const reports=computed(()=>reportEvents(props.task).events)
const latest=computed(()=>reports.value.at(-1))
const workItems=computed<Row[]>(()=>props.task.workItems??[])
const completed=computed(()=>workItems.value.filter(item=>item.status==='COMPLETED').length)
</script>
<template>
 <section class="overview panel" aria-labelledby="task-progress-heading">
  <header><p class="eyebrow">TASK PROGRESS</p><h2 id="task-progress-heading">任务进展</h2></header>
  <div class="latest-report"><p class="report-source">{{ latest?.source||'最新执行记录' }}<template v-if="latest?.department"> · {{ latest.department }}</template></p><p v-if="latest" class="detail-text">{{ latest.content }}</p><p v-else class="empty-note">尚未填写执行进展</p><small v-if="latest">{{ date(latest.createdAt) }}<template v-if="latest.author"> · {{ latest.author }}</template></small></div>
  <div v-if="workItems.length" class="work-item-summary"><span>可见子任务</span><strong>{{ completed }} / {{ workItems.length }} 已完成</strong></div>
  <el-button text type="primary" class="history-link" @click="emit('history')">查看进度记录 →</el-button>
 </section>
</template>
<style scoped>
.overview{min-width:0;margin:0}.overview header{margin-bottom:16px}.overview h2{margin:0}.latest-report{padding:14px;border:1px solid var(--line);border-radius:8px;background:var(--surface-muted)}.report-source{font-size:11px;font-weight:600;color:var(--muted);margin:0 0 8px;overflow-wrap:anywhere}.detail-text{margin:0 0 10px;font-size:13px;line-height:1.8;white-space:pre-wrap;overflow-wrap:anywhere;max-height:320px;overflow-y:auto}.latest-report small,.empty-note{font-size:12px;color:var(--muted)}.empty-note{margin:0;line-height:1.7}.work-item-summary{display:flex;justify-content:space-between;gap:10px;flex-wrap:wrap;border-top:1px solid var(--line);padding-top:14px;margin-top:16px;font-size:12px}.work-item-summary span{color:var(--muted)}.history-link{margin-top:12px;padding-left:0}.history-link:focus-visible{outline:2px solid var(--brand);outline-offset:3px}
</style>
