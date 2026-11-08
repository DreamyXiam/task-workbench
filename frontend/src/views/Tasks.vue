<script setup lang="ts">
import { computed,onMounted,ref,watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { roles } from '../domain'
import type { Page,Row } from '../domain'
import { useAuth } from '../stores/auth'
import { useLoad,usePoll } from '../composables'
import PageState from '../components/PageState.vue'
import TaskCard from '../components/TaskCard.vue'
import TaskGantt from '../components/TaskGantt.vue'
const view=ref<'cards'|'gantt'>('cards')
const route=useRoute(),auth=useAuth(),items=ref<Row[]>([]),total=ref(0),page=ref(1),pageSize=ref(20),keyword=ref(''),status=ref(String(route.query.status||'')),late=ref(route.query.overdue==='true'),priority=ref(''),keyWork=ref(false),filterOpen=ref(false),{loading,error,run}=useLoad()
const mine=computed(()=>route.path==='/mine'),leader=computed(()=>(['GROUP_LEADER','DIVISION_LEADER','COMPANY_LEADER'].includes(auth.me?.role??'')||(auth.me?.role==='ADMIN'&&auth.me?.companyId==null))&&(!auth.me?.permissions||auth.me.permissions.includes('TASK_CREATE')))
const tabs=computed(()=>mine.value?[['','全部任务'],['PENDING_RECEIVE','待接收'],['IN_PROGRESS','进行中'],['SUBMITTED','已提交'],['ACCEPTED','已验收'],['COMPLETED','已完成']]:[['','全部任务'],['DRAFT','待确认'],['IN_PROGRESS','进行中'],['PENDING_ACCEPTANCE','待验收'],['COMPLETED','已完成']])
function load(){run(()=>api.get<Page>(mine.value?'/sub-tasks/mine':'/tasks',{page:page.value,pageSize:pageSize.value,keyword:keyword.value,status:status.value||undefined,priority:priority.value||undefined,overdue:late.value||undefined,keyWork:!mine.value&&keyWork.value||undefined}),data=>{items.value=data.items;total.value=data.total})}
function search(){page.value=1;load();filterOpen.value=false}
function choose(value:string){status.value=value;search()}
watch(()=>[route.path,route.query.status,route.query.overdue],()=>{page.value=1;status.value=String(route.query.status||'');late.value=route.query.overdue==='true';load()});onMounted(load);usePoll(()=>{if(!loading.value&&!filterOpen.value)load()})
</script>
<template>
<section class="page-heading"><div><p class="eyebrow">TASK MANAGEMENT</p><h1>{{ mine?'我的任务':'任务中心' }}</h1><p class="muted">{{ mine?'按执行责任聚焦当前工作':'统一跟踪从布置到验收的完整流程' }} · {{ roles[auth.me?.role||''] }}</p></div><div v-if="leader&&!mine" class="action-strip"><router-link to="/tasks/import"><el-button>批量布置</el-button></router-link><router-link to="/tasks/new"><el-button type="primary">＋ 布置任务</el-button></router-link></div></section><section class="panel task-panel">
<div class="status-tabs" role="tablist" aria-label="任务状态"><button v-for="t in tabs" :key="t[0]" role="tab" :aria-selected="status===t[0]" :class="{selected:status===t[0]}" @click="choose(t[0])">{{ t[1] }}</button></div><div v-if="!mine" class="action-strip"><el-checkbox v-model="keyWork" @change="search">重点工作汇总</el-checkbox></div><form class="filter-bar" @submit.prevent="search"><el-input v-model="keyword" clearable placeholder="搜索任务名称 / 编号" aria-label="搜索任务名称或编号"/><div class="desktop-filters"><el-select v-model="priority" clearable placeholder="优先级" aria-label="优先级"><el-option label="紧急" value="URGENT"/><el-option label="高" value="HIGH"/><el-option label="普通" value="NORMAL"/><el-option label="低" value="LOW"/></el-select><el-checkbox v-model="late">仅看逾期</el-checkbox></div><el-button type="primary" plain native-type="submit">查询</el-button><el-button class="mobile-only" @click="filterOpen=true">筛选</el-button><el-button class="desktop-only" @click="keyword='';priority='';late=false;status='';search()">重置</el-button></form><div class="list-caption"><span>共 {{ total }} 项任务</span><span>逾期为独立标记 · 每30秒自动更新</span></div>
<PageState :loading="loading" :error="error" :empty="!items.length" @retry="load">
<div v-if="!mine" class="view-switch" role="group" aria-label="任务展示方式"><button :aria-pressed="view==='cards'" @click="view='cards'">卡片</button><button :aria-pressed="view==='gantt'" @click="view='gantt'">甘特图</button></div>
<TaskGantt v-if="!mine&&view==='gantt'" :tasks="items"/>
<div v-else class="task-card-grid"><TaskCard v-for="row in items" :key="row.kind+':'+row.id" :task="row" :show-key-work="keyWork&&!mine"/></div>
</PageState><div class="pagination"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="prev, pager, next" @current-change="load" @size-change="search"/></div>
</section><el-dialog v-model="filterOpen" align-center class="app-card-dialog" title="筛选任务" width="520px"><el-form label-position="top"><el-form-item label="优先级"><el-select v-model="priority" clearable><el-option label="紧急" value="URGENT"/><el-option label="高" value="HIGH"/><el-option label="普通" value="NORMAL"/><el-option label="低" value="LOW"/></el-select></el-form-item><el-checkbox v-model="late">仅看逾期任务</el-checkbox><p><el-button type="primary" @click="search">应用筛选</el-button></p></el-form></el-dialog>
</template>

<style scoped>
.view-switch{display:flex;gap:3px;border:1px solid var(--line);border-radius:8px;background:var(--surface-muted);padding:4px;width:fit-content;margin-bottom:16px}.view-switch button{border:0;background:transparent;color:var(--muted);padding:7px 18px;border-radius:5px;cursor:pointer;font:inherit;font-size:13px}.view-switch button[aria-pressed=true]{background:var(--surface);color:var(--text);box-shadow:0 1px 3px #0002}.view-switch button:focus-visible{outline:2px solid var(--brand)}
.task-card-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,340px),1fr));gap:18px}.task-panel{background:transparent;box-shadow:none;border:0;padding:0}.filter-bar{padding:16px;background:var(--surface);border:1px solid var(--line);border-radius:10px}.list-caption{padding:4px 2px 16px}
</style>
