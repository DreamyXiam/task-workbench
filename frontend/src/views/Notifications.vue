<script setup lang="ts">
import { onMounted,ref } from 'vue'
import { api,errorText } from '../api'
import { date,listItems } from '../domain'
import type { Row,Page } from '../domain'
import { useLoad,usePoll } from '../composables'
import PageState from '../components/PageState.vue'
const {loading,error,run}=useLoad(),items=ref<Row[]>([]),page=ref(1),total=ref(0),filter=ref(''),operationError=ref(''),busy=ref(false)
function load(){run(()=>api.get<Page>('/notifications',{page:page.value,pageSize:20,read:filter.value||undefined}),v=>{items.value=listItems(v);total.value=v.total??items.value.length})}
async function read(n:Row){operationError.value='';try{await api.post(`/notifications/${n.id}/read`);load()}catch(e){operationError.value=errorText(e)}}
async function archive(){busy.value=true;try{await api.post('/notifications/archive');load()}catch(e){operationError.value=errorText(e)}finally{busy.value=false}}
onMounted(load);usePoll(()=>{if(!loading.value)load()})
</script>
<template><section class="page-heading"><div><p class="eyebrow">NOTIFICATION CENTER</p><h1>通知中心</h1><p class="muted">任务变更、协同申请与重要节点提醒。</p></div><el-button :loading="busy" @click="archive">归档已读通知</el-button></section><el-alert v-if="operationError" :title="operationError" type="error" :closable="false"/><section class="panel"><div class="status-tabs"><button :class="{selected:filter===''}" @click="filter='';page=1;load()">全部通知</button><button :class="{selected:filter==='false'}" @click="filter='false';page=1;load()">未读</button><button :class="{selected:filter==='true'}" @click="filter='true';page=1;load()">已读</button></div><PageState :loading="loading" :error="error" :empty="!items.length" @retry="load"><article v-for="n in items" :key="n.id" class="notification-item" :class="{unread:!n.readAt&&!n.isRead}"><div class="notification-mark">{{ n.readAt||n.isRead?'知':'新' }}</div><div class="notification-body"><h3>{{ n.title||'工作通知' }}</h3><p class="pre-wrap">{{ n.content||n.message }}</p><small>{{ date(n.createdAt) }}</small><router-link v-if="n.taskId" :to="'/tasks/'+n.taskId" class="text-link">查看任务 →</router-link></div><el-button v-if="!n.readAt&&!n.isRead" text @click="read(n)">标为已读</el-button></article></PageState><div class="pagination"><el-pagination v-model:current-page="page" :total="total" :page-size="20" layout="prev,pager,next" @current-change="load"/></div></section></template>
