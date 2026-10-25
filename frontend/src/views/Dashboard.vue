<script setup lang="ts">
import { computed,onMounted,ref } from 'vue'
import { useAuth } from '../stores/auth'
import { api } from '../api'
import { date,roles } from '../domain'
import type { Row } from '../domain'
import { useLoad,usePoll } from '../composables'
import PageState from '../components/PageState.vue'
import StatusBadge from '../components/StatusBadge.vue'
const auth=useAuth(),overview=ref<Row>({}),{loading,error,run}=useLoad()
const leader=computed(()=>['GROUP_LEADER','DIVISION_LEADER','COMPANY_LEADER'].includes(auth.me?.role??''))
const cards=[['total','任务总数','当前授权范围'],['inProgress','进行中','执行跟进'],['pendingAcceptance','待验收','成果核验'],['overdue','逾期任务','需重点关注']]
function load(){run(()=>api.get('/stats/overview'),v=>overview.value=v)}
onMounted(load);usePoll(load)
</script>
<template>
<section class="page-heading"><div><p class="eyebrow">WORKSPACE OVERVIEW</p><h1>{{ leader?'领导工作概览':auth.me?.role==='DEPT_HEAD'?'部门工作台':auth.me?.role==='EMPLOYEE'?'我的工作台':'管理工作台' }}</h1><p class="muted">{{ auth.me?.name }}，您好。{{ roles[auth.me?.role||''] }} · 当前组织范围内的任务与协同。</p></div><router-link v-if="leader" to="/tasks/new"><el-button type="primary" size="large">＋ 布置任务</el-button></router-link><el-button v-else @click="load">刷新概览</el-button></section>
<PageState :loading="loading" :error="error" @retry="load">
<div class="metric-grid"><router-link v-for="(c,i) in cards" :key="c[0]" :to="'/tasks'+(i===2?'?status=PENDING_ACCEPTANCE':i===3?'?overdue=true':'')" class="metric-card" :class="{'metric-warning':i===3}"><div class="metric-label">{{ c[1] }}<span>{{ String(i+1).padStart(2,'0') }}</span></div><strong>{{ overview[c[0]]??'—' }}</strong><small>{{ c[2] }}</small></router-link></div>
<div class="dashboard-grid"><section class="panel"><div class="panel-heading"><div><h2>近期任务</h2><p class="muted">关注最新进展，及时处理关键节点</p></div><router-link to="/tasks" class="text-link">查看全部 →</router-link></div><el-empty v-if="!overview.recentTasks?.length" description="暂无近期任务"/><router-link v-for="t in overview.recentTasks" :key="t.id" :to="'/tasks/'+t.id" class="task-line"><div class="task-index">{{ t.code?.slice(-2)||'任' }}</div><div class="task-line-body"><strong>{{ t.title }}</strong><p>{{ t.code }} · 截止 {{ date(t.deadline) }}</p></div><StatusBadge :status="t.status"/></router-link></section><section class="panel attention-panel"><p class="eyebrow">FOCUS & FOLLOW UP</p><h2>让每项任务都有回响</h2><p class="muted">任务节点清楚，执行过程留痕，成果统一验收。</p><router-link to="/mine" class="quick-action"><b>我的待办</b><span>接收、执行、提交 →</span></router-link><router-link to="/collaborations" class="quick-action"><b>跨公司协同</b><span>确认协作范围与责任 →</span></router-link><router-link to="/notifications" class="quick-action"><b>通知与提醒</b><span>掌握变更和截止节点 →</span></router-link><p class="scope-note">数据按当前岗位与组织范围展示。切换岗位后将重新加载。</p></section></div>
</PageState>
</template>
