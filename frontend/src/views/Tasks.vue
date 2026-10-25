<script setup lang="ts">
import { computed,onMounted,ref,watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { date,overdue,roles } from '../domain'
import type { Page,Row } from '../domain'
import { useAuth } from '../stores/auth'
import { useLoad,usePoll } from '../composables'
import PageState from '../components/PageState.vue'
import StatusBadge from '../components/StatusBadge.vue'
const route=useRoute(),auth=useAuth(),items=ref<Row[]>([]),total=ref(0),page=ref(1),pageSize=ref(20),keyword=ref(''),status=ref(String(route.query.status||'')),late=ref(route.query.overdue==='true'),priority=ref(''),filterOpen=ref(false),{loading,error,run}=useLoad()
const mine=computed(()=>route.path==='/mine'),leader=computed(()=>['GROUP_LEADER','DIVISION_LEADER','COMPANY_LEADER'].includes(auth.me?.role??''))
const tabs=[['','全部任务'],['PENDING_CONFIRM','待确认'],['IN_PROGRESS','进行中'],['PENDING_ACCEPTANCE','待验收'],['COMPLETED','已完成']]
function load(){run(()=>api.get<Page>(mine.value?'/sub-tasks/mine':'/tasks',{page:page.value,pageSize:pageSize.value,keyword:keyword.value,status:status.value||undefined,priority:priority.value||undefined,overdue:late.value||undefined}),data=>{items.value=data.items;total.value=data.total})}
function search(){page.value=1;load();filterOpen.value=false}
function choose(value:string){status.value=value;search()}
watch(()=>route.path,()=>{page.value=1;load()});onMounted(load);usePoll(()=>{if(!loading.value&&!filterOpen.value)load()})
</script>
<template>
<section class="page-heading"><div><p class="eyebrow">TASK MANAGEMENT</p><h1>{{ mine?'我的任务':'任务中心' }}</h1><p class="muted">{{ mine?'按执行责任聚焦当前工作':'统一跟踪从布置到验收的完整流程' }} · {{ roles[auth.me?.role||''] }}</p></div><router-link v-if="leader&&!mine" to="/tasks/new"><el-button type="primary">＋ 布置任务</el-button></router-link></section><section class="panel task-panel">
<div class="status-tabs" role="tablist" aria-label="任务状态"><button v-for="t in tabs" :key="t[0]" role="tab" :aria-selected="status===t[0]" :class="{selected:status===t[0]}" @click="choose(t[0])">{{ t[1] }}</button></div><form class="filter-bar" @submit.prevent="search"><el-input v-model="keyword" clearable placeholder="搜索任务名称 / 编号" aria-label="搜索任务名称或编号"/><div class="desktop-filters"><el-select v-model="priority" clearable placeholder="优先级" aria-label="优先级"><el-option label="紧急" value="URGENT"/><el-option label="高" value="HIGH"/><el-option label="普通" value="NORMAL"/></el-select><el-checkbox v-model="late">仅看逾期</el-checkbox></div><el-button type="primary" plain native-type="submit">查询</el-button><el-button class="mobile-only" @click="filterOpen=true">筛选</el-button><el-button class="desktop-only" @click="keyword='';priority='';late=false;status='';search()">重置</el-button></form><div class="list-caption"><span>共 {{ total }} 项任务</span><span>逾期为独立标记 · 每30秒自动更新</span></div>
<PageState :loading="loading" :error="error" :empty="!items.length" @retry="load">
<el-table class="desktop-table" :data="items" row-key="id" @row-click="(row:Row)=>$router.push('/tasks/'+(row.taskId||row.id))"><el-table-column label="任务名称" min-width="280"><template #default="{row}"><span class="table-code">{{ row.code }}</span><router-link :to="'/tasks/'+(row.taskId||row.id)" class="task-title-link">{{ row.title||row.taskTitle }}</router-link></template></el-table-column><el-table-column label="状态" min-width="200"><template #default="{row}"><StatusBadge :status="row.status" :overdue="overdue(row)"/></template></el-table-column><el-table-column label="优先级" min-width="85"><template #default="{row}"><span :class="{'priority-high':['HIGH','URGENT'].includes(row.priority)}">{{ ({URGENT:'紧急',HIGH:'高',NORMAL:'普通',LOW:'低'} as Record<string,string>)[row.priority]||row.priority||'—' }}</span></template></el-table-column><el-table-column label="截止时间" min-width="160"><template #default="{row}">{{ date(row.deadline) }}</template></el-table-column><el-table-column label="主办部门" prop="hostDeptName" min-width="115"/></el-table>
<div class="mobile-task-cards"><router-link v-for="row in items" :key="row.id" :to="'/tasks/'+(row.taskId||row.id)" class="mobile-task-card"><small>{{ row.code }}</small><h3>{{ row.title||row.taskTitle }}</h3><StatusBadge :status="row.status" :overdue="overdue(row)"/><p>截止 {{ date(row.deadline) }}</p><span>查看详情 →</span></router-link></div>
</PageState><div class="pagination"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="prev, pager, next" @current-change="load" @size-change="search"/></div>
</section><el-drawer v-model="filterOpen" title="筛选任务" size="85%"><el-form label-position="top"><el-form-item label="优先级"><el-select v-model="priority" clearable><el-option label="紧急" value="URGENT"/><el-option label="高" value="HIGH"/><el-option label="普通" value="NORMAL"/></el-select></el-form-item><el-checkbox v-model="late">仅看逾期任务</el-checkbox><p><el-button type="primary" @click="search">应用筛选</el-button></p></el-form></el-drawer>
</template>
