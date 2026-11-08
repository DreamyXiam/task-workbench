<script setup lang="ts">
import { computed,onMounted,ref } from 'vue'
import { useAuth } from '../stores/auth'
import { api } from '../api'
import { date,roles } from '../domain'
import type { Row } from '../domain'
import { useLoad,usePoll } from '../composables'
import PageState from '../components/PageState.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { summaryValue } from '../key-work'
import { ListTodo, CircleDot, ClipboardCheck, CircleAlert } from 'lucide-vue-next'
const auth=useAuth(),overview=ref<Row>({}),focus=ref('all'),{loading,error,run}=useLoad()
const leader=computed(()=>['GROUP_LEADER','DIVISION_LEADER','COMPANY_LEADER'].includes(auth.me?.role??''))
const canView=computed(()=>!auth.me?.permissions||auth.me.permissions.includes('TASK_VIEW'))
const canCreate=computed(()=>leader.value&&(!auth.me?.permissions||auth.me.permissions.includes('TASK_CREATE')))
const cardIcons=[ListTodo,CircleDot,ClipboardCheck,CircleAlert]
const cards=[['total','任务总数','当前授权范围'],['inProgress','进行中','执行跟进'],['pendingAcceptance','待验收','成果核验'],['overdue','逾期任务','需重点关注']]
function load(){if(!canView.value)return;run(()=>api.get('/stats/overview',{focus:focus.value}),v=>overview.value=v)}
function selectFocus(value:string){focus.value=value;load()}
function detailField(task:Row,id:string){const value=summaryValue(task.keyWorkData,id);return value==='—'?'':String(value)}
onMounted(load);usePoll(load)
</script>
<template>
<section class="page-heading"><div><p class="eyebrow">WORKSPACE OVERVIEW</p><h1>{{ leader?'领导工作概览':auth.me?.role==='DEPT_HEAD'?'部门工作台':auth.me?.role==='EMPLOYEE'?'我的工作台':'管理工作台' }}</h1><p class="muted">{{ auth.me?.name }}，您好。{{ roles[auth.me?.role||''] }} · 当前组织范围内的任务与协同。</p></div><router-link v-if="canCreate" to="/tasks/new"><el-button type="primary" size="large">＋ 布置任务</el-button></router-link><el-button v-else-if="canView" @click="load">刷新概览</el-button></section>
<el-alert v-if="!canView" title="当前角色未开启任务查看权限" type="info" :closable="false"/><PageState v-else :loading="loading" :error="error" @retry="load">
<div class="metric-grid"><router-link v-for="(c,i) in cards" :key="c[0]" :to="'/tasks'+(i===2?'?status=PENDING_ACCEPTANCE':i===3?'?overdue=true':'')" class="metric-card" :class="{'metric-warning':i===3}"><div class="metric-label">{{ c[1] }}<component :is="cardIcons[i]" :size="16" aria-hidden="true"/></div><strong>{{ overview[c[0]]??'—' }}</strong><small>{{ c[2] }}</small></router-link></div>
<div class="dashboard-grid"><section class="panel"><div class="panel-heading"><div><h2>任务进展与重点关注</h2><p class="muted">优先展示需协调、临近截止和长期未更新的任务</p></div><router-link to="/tasks" class="text-link">查看全部 →</router-link></div>
<div class="focus-tabs" role="group" aria-label="首页任务筛选"><button v-for="option in [{id:'all',label:'全部'},{id:'attention',label:'需关注'},{id:'active',label:'进行中'}]" :key="option.id" type="button" :class="{selected:focus===option.id}" :aria-pressed="focus===option.id" @click="selectFocus(option.id)">{{ option.label }}</button></div>
<el-empty v-if="!overview.recentTasks?.length" description="当前筛选下暂无任务"/>
<router-link v-for="t in overview.recentTasks" :key="t.id" :to="'/tasks/'+t.id" class="progress-task"><div class="progress-title"><strong>{{ t.title }}</strong><StatusBadge :status="t.status"/></div><p class="progress-meta">{{ t.code }} · {{ t.hostDeptName||'主办部门未设置' }} · 负责人：{{ t.hostOwnerName||(t.hostVisible?'待分派':'当前岗位不可见') }} · 截止 {{ date(t.deadline) }}</p><div v-if="t.attentionFlags?.length" class="focus-flags"><span v-for="flag in t.attentionFlags" :key="flag">{{ flag }}</span></div><p class="progress-copy"><b>{{ t.progressSource||'最新进展' }}{{ t.progressDeptName?'（'+t.progressDeptName+'）':'' }}：</b>{{ t.latestProgress||'暂无进展记录' }}</p><p class="progress-meta"><span v-if="t.progressAt">更新于 {{ date(t.progressAt) }}</span><span v-if="t.workItemTotal"> · 可见子任务 {{ t.workItemCompleted }} / {{ t.workItemTotal }} 已完成</span></p><p v-if="detailField(t,'problem')" class="issue-copy"><b>存在问题：</b>{{ detailField(t,'problem') }}</p><p v-if="detailField(t,'coordination')" class="issue-copy"><b>需领导协调：</b>{{ detailField(t,'coordination') }}</p></router-link></section><section class="panel attention-panel"><p class="eyebrow">FOCUS & FOLLOW UP</p><h2>让每项任务都有回响</h2><p class="muted">任务节点清楚，执行过程留痕，成果统一验收。</p><router-link to="/mine" class="quick-action"><b>我的待办</b><span>接收、执行、提交 →</span></router-link><router-link v-if="!auth.me?.permissions||auth.me.permissions.includes('COLLABORATION')" to="/collaborations" class="quick-action"><b>跨公司协同</b><span>确认协作范围与责任 →</span></router-link><router-link v-if="!auth.me?.permissions||auth.me.permissions.includes('NOTIFICATIONS')" to="/notifications" class="quick-action"><b>通知与提醒</b><span>掌握变更和截止节点 →</span></router-link><p class="scope-note">数据按当前岗位与组织范围展示。切换岗位后将重新加载。</p></section></div>
</PageState>
</template>
<style scoped>
.metric-card { text-align: left; }
.metric-label { justify-content: space-between; gap: 12px; }
.metric-label svg { flex-shrink: 0; color: var(--muted); }
.focus-tabs{display:flex;gap:8px;margin-bottom:8px}.focus-tabs button{border:1px solid var(--line);background:#fff;color:var(--muted);border-radius:16px;padding:6px 14px;cursor:pointer}.focus-tabs button.selected{background:#e9f2fa;color:var(--brand);border-color:#bed7ec}.progress-task{display:block;padding:18px 0;border-bottom:1px solid var(--line);color:inherit;min-width:0}.progress-task:last-child{border-bottom:0}.progress-task:hover strong{color:var(--brand)}.progress-title{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.progress-title strong{overflow-wrap:anywhere}.progress-meta{font-size:12px;color:var(--muted);line-height:1.7;margin:7px 0}.progress-copy,.issue-copy{font-size:13px;line-height:1.65;margin:8px 0;white-space:pre-wrap;overflow-wrap:anywhere}.progress-copy{display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.issue-copy{color:#9a570d}.focus-flags{display:flex;gap:6px;flex-wrap:wrap;margin:8px 0}.focus-flags span{font-size:11px;border-radius:4px;padding:3px 7px;background:#fff3e2;color:#a35d11}.progress-bar{max-width:320px;margin:8px 0}@media(max-width:767px){.progress-title{gap:8px}.progress-meta{font-size:11px}.focus-tabs button{padding:6px 11px}}
</style>
