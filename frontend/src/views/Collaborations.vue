<script setup lang="ts">
import { onMounted,ref } from 'vue'
import { api } from '../api'
import { allowed,date,listItems } from '../domain'
import type { Row } from '../domain'
import { useLoad } from '../composables'
import PageState from '../components/PageState.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ActionDialog from '../components/ActionDialog.vue'
const items=ref<Row[]>([]),{loading,error,run}=useLoad(),dialog=ref(false),row=ref<Row>({}),action=ref('')
function load(){run(()=>api.get('/collaborations'),v=>items.value=listItems(v))}
function act(r:Row,a:string){row.value=r;action.value=a;dialog.value=true}
onMounted(load)
</script>
<template><section class="page-heading"><div><p class="eyebrow">CROSS COMPANY COLLABORATION</p><h1>协同确认</h1><p class="muted">确认协办责任，建立跨公司任务协作关系。</p></div><el-button @click="load">刷新</el-button></section><section class="panel"><p class="notice">确认前仅展示必要的协作信息。确认后可按授权范围进入任务执行。</p><PageState :loading="loading" :error="error" :empty="!items.length" @retry="load"><article v-for="r in items" :key="r.id" class="collaboration-card"><div class="row-between"><h2>{{ r.taskTitle||r.title||'协办确认' }}</h2><StatusBadge :status="r.status"/></div><p class="muted">{{ r.fromCompanyName||r.companyId }} → {{ r.deptName||r.deptId }} · {{ date(r.createdAt) }}</p><p class="pre-wrap">{{ r.content||r.reason||r.message }}</p><div class="action-strip"><el-button v-if="allowed(r,'accept')" type="primary" @click="act(r,'accept')">接受协办</el-button><el-button v-if="allowed(r,'reject')" @click="act(r,'reject')">拒绝并说明原因</el-button><router-link v-if="allowed(r,'view-task')&&r.taskId" :to="'/tasks/'+r.taskId">查看任务 →</router-link></div></article></PageState></section><ActionDialog v-model="dialog" :row="row" :action="action" resource="collaborations" @done="load"/></template>
