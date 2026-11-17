<script setup lang="ts">
import { computed } from 'vue'
import { CalendarDays,Building2,UserRound,ArrowUpRight,TriangleAlert } from 'lucide-vue-next'
import { date,overdue } from '../domain'
import type { Row } from '../domain'
import { summaryValue } from '../key-work'
import StatusBadge from './StatusBadge.vue'
const props=defineProps<{task:Row;showKeyWork?:boolean}>()
const priority=computed(()=>({HIGH:'高优先级',URGENT:'紧急',NORMAL:'普通',LOW:'低优先级'} as Record<string,string>)[props.task.priority]??'未设置优先级')
const lateLabel=computed(()=>{if(!overdue(props.task))return '';const active=!['COMPLETED','ACCEPTED','CANCELLED'].includes(props.task.status);return active&&new Date(props.task.deadline).getTime()<Date.now()?'已逾期':'曾逾期'})
const attention=computed(()=>props.task.attentionFlags?.filter((flag:string)=>flag!=='已逾期'||!lateLabel.value)??[])
</script>
<template>
 <router-link :to="'/tasks/'+(task.taskId||task.id)" class="task-card" :class="{'is-overdue':overdue(task)}" :aria-label="'查看任务 '+(task.title||task.taskTitle)">
  <div class="card-top"><span class="card-code">{{ task.code||'未编号' }}</span><span class="card-priority" :class="{'priority-high':['HIGH','URGENT'].includes(task.priority)}">{{ priority }}</span></div>
  <h2 :title="task.title||task.taskTitle">{{ task.title||task.taskTitle }}</h2>
  <div class="card-state"><StatusBadge :status="task.status"/><span v-if="task.taskId&&task.title!==task.taskTitle" class="card-parent">{{ task.taskTitle }}</span></div>
  <div v-if="attention.length" class="card-flags"><span v-for="flag in attention" :key="flag" :class="{urgent:flag.includes('逾期')||flag.includes('未见进展')}"><TriangleAlert :size="13" aria-hidden="true"/>{{ flag }}</span></div>
  <dl class="card-meta"><div><dt><Building2 :size="14" aria-hidden="true"/>责任部门</dt><dd>{{ task.hostDeptName||task.deptName||'未指定部门' }}</dd></div><div><dt><UserRound :size="14" aria-hidden="true"/>负责人</dt><dd>{{ task.ownerName||task.hostOwnerName||(task.hostVisible===false?'当前岗位不可见':'待分派') }}</dd></div><div><dt><CalendarDays :size="14" aria-hidden="true"/>截止时间</dt><dd>{{ date(task.deadline) }} <strong v-if="lateLabel" class="deadline-alert"><TriangleAlert :size="13" aria-hidden="true"/>{{ lateLabel }}</strong></dd></div></dl>
  <div class="card-update"><div class="update-heading"><b>{{ task.progressSource||'最新进展' }}<span v-if="task.progressDeptName"> · {{ task.progressDeptName }}</span></b></div><p :class="{'no-progress':!task.latestProgress}" :title="task.latestProgress||undefined">{{ task.latestProgress||'暂无进展记录' }}</p><small v-if="task.progressAt">更新于 {{ date(task.progressAt) }}</small></div>
  <div v-if="showKeyWork" class="card-key-work"><p v-for="f in [{id:'progress',name:'推进情况'},{id:'problem',name:'存在问题'},{id:'suggestion',name:'解决建议'},{id:'coordination',name:'需领导协调'},{id:'remark',name:'备注'}]" :key="f.id"><b>{{ f.name }}：</b>{{ summaryValue(task.keyWorkData,f.id) }}</p><p><b>协调状态：</b>{{ ({NONE:'无需协调',PENDING:'待协调',IN_PROGRESS:'协调中',RESOLVED:'已解决'} as Row)[task.keyWorkData?.coordinationStatus||'NONE'] }}</p><p><b>领导反馈：</b>{{ task.keyWorkData?.feedback||'—' }}</p></div>
  <footer class="card-footer"><span class="card-footer-meta"><span class="card-company" :title="task.companyName||'当前授权范围'">{{ task.companyName||'当前授权范围' }}</span><span v-if="task.workItemTotal" class="card-work-items">· 子任务 {{ task.workItemCompleted }} / {{ task.workItemTotal }}</span></span><span class="card-details">查看详情 <ArrowUpRight :size="14" aria-hidden="true"/></span></footer>
 </router-link>
</template>
<style scoped>
.task-card{color:var(--text);text-decoration:none;cursor:pointer;display:flex;flex-direction:column;min-width:0;height:100%;padding:22px;border:1px solid var(--line);border-radius:12px;background:var(--surface);box-shadow:var(--shadow-card);transition:border-color .15s,box-shadow .15s}
.task-card:hover,.task-card:focus-within{border-color:var(--brand);box-shadow:0 4px 16px rgb(0 0 0 / 7%)}
.task-card.is-overdue{border-color:color-mix(in srgb,var(--danger) 44%,var(--line));border-top:3px solid var(--danger)}
.task-card:focus-visible{outline:2px solid var(--brand);outline-offset:4px}
.card-top,.card-state,.card-footer{display:flex;align-items:center;justify-content:space-between;gap:10px}
.card-code,.card-parent,.card-meta,.card-footer,small{font-size:12px;color:var(--muted)}
.card-code{overflow-wrap:anywhere;opacity:.8}
.card-priority{display:inline-flex;align-items:center;flex-shrink:0;border:1px solid var(--line);border-radius:5px;background:var(--surface-muted);padding:3px 7px;font-size:11px;line-height:1.4;color:var(--muted)}
.card-priority.priority-high{border-color:color-mix(in srgb,var(--danger) 28%,var(--line));background:color-mix(in srgb,var(--danger) 11%,var(--surface));color:var(--danger);font-weight:650}
.task-card h2{font-size:18px;line-height:1.45;font-weight:700;margin:12px 0 10px;min-height:2.9em;overflow-wrap:anywhere;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden}
.card-state{justify-content:flex-start;flex-wrap:wrap}
.task-card .card-state :deep(.status){border:0;background:transparent;padding:0;font-size:12px;font-weight:650}
.card-state :deep(.status-dot){width:6px;height:6px}
.card-flags{display:flex;flex-wrap:wrap;gap:7px;margin-top:12px}
.card-flags span{display:inline-flex;align-items:center;gap:4px;font-size:11px;font-weight:600;padding:3px 7px;border-radius:5px;background:color-mix(in srgb,var(--warning) 11%,var(--surface));color:var(--warning)}
.card-flags span.urgent{background:color-mix(in srgb,var(--danger) 11%,var(--surface));color:var(--danger)}
.card-meta{display:grid;gap:9px;margin:17px 0 auto;padding-bottom:17px}
.card-meta div{display:grid;grid-template-columns:82px minmax(0,1fr);align-items:start;gap:10px}
.card-meta dt{display:flex;align-items:center;gap:6px;color:var(--muted);white-space:nowrap}
.card-meta dd{margin:0;color:var(--text);overflow-wrap:anywhere;line-height:1.55}
.deadline-alert{display:inline-flex;align-items:center;gap:3px;margin-left:5px;color:var(--danger);font-size:11px;white-space:nowrap}
.card-update{border-top:1px dashed var(--line);padding:14px 0 0;margin-bottom:16px;min-height:114px}
.update-heading{font-size:12px}.update-heading b{font-weight:650}.update-heading b span{font-weight:400;color:var(--muted)}
.card-update p{font-size:13px;margin:9px 0 5px;min-height:3.2em;line-height:1.6;color:var(--muted);white-space:pre-wrap;overflow-wrap:anywhere;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2;overflow:hidden}
.card-update .no-progress{color:var(--muted)}.card-update small{display:block;color:var(--muted)}
.card-key-work{font-size:12px;white-space:pre-wrap;overflow-wrap:anywhere}.card-key-work p{border-bottom:1px solid var(--line);padding-bottom:8px}
.card-footer{margin-top:auto;padding-top:14px;border-top:1px solid var(--line)}
.card-footer-meta{display:flex;align-items:center;gap:4px;min-width:0;flex:1}.card-company{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.card-work-items{flex-shrink:0;white-space:nowrap}
.card-details{display:flex;align-items:center;gap:4px;flex-shrink:0;color:var(--brand);font-weight:600;white-space:nowrap;transition:color .15s}
.card-details svg{transition:transform .15s}.task-card:hover .card-details,.task-card:focus-visible .card-details{color:var(--brand-dark);text-decoration:underline}.task-card:hover .card-details svg,.task-card:focus-visible .card-details svg{transform:translate(2px,-2px)}
@media(max-width:767px){.task-card{padding:18px}}
@media(prefers-reduced-motion:reduce){.task-card,.card-details,.card-details svg{transition:none}}
</style>
