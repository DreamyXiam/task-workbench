<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { api,errorText } from '../api'
import { useAuth } from '../stores/auth'
import { date,listItems } from '../domain'
import type { Row } from '../domain'
const auth=useAuth()
const props=defineProps<{task:Row}>(),emit=defineEmits<{changed:[]}>()
const dialog=ref(false),busy=ref(false),error=ref(''),members=ref<Row[]>([]),percent=reactive<Record<string,number>>({})
const form=ref<Row>({title:'',deptId:null,ownerId:null,deadline:''})
watch(()=>form.value.deptId,async dept=>{form.value.ownerId=null;members.value=[];if(dept==null)return;try{members.value=listItems(await api.get(`/tasks/${props.task.id}/work-item-members`,{deptId:dept}))}catch(e){error.value=errorText(e)}})
function open(){error.value='';form.value={title:'',deptId:props.task.workItemDepartments?.[0]?.id??null,ownerId:null,deadline:''};dialog.value=true}
async function create(){if(busy.value)return;error.value='';if(!form.value.title.trim()||form.value.deptId==null||form.value.ownerId==null||!form.value.deadline){error.value='请填写名称、部门、负责人和截止时间';return}busy.value=true;try{await api.post(`/tasks/${props.task.id}/work-items`,form.value);dialog.value=false;emit('changed')}catch(e){error.value=errorText(e)}finally{busy.value=false}}
async function update(item:Row){if(busy.value)return;busy.value=true;error.value='';try{await api.post(`/work-items/${item.id}`,{version:item.version,percent:percent[String(item.id)]??item.percent});delete percent[String(item.id)];emit('changed')}catch(e){error.value=errorText(e)}finally{busy.value=false}}
</script>
<template>
<section class="work-items">
<div class="row-between"><div><h2>子任务</h2><p class="muted">将部门工作拆分为可独立跟踪的事项。</p></div><el-button v-if="task.workItemDepartments?.length&&(!auth.me?.permissions||auth.me.permissions.includes('TASK_UPDATE'))" type="primary" plain @click="open">＋ 新建子任务</el-button></div>
<el-alert v-if="error&&!dialog" :title="error" type="error" :closable="false"/>
<el-empty v-if="!task.workItems?.length" description="暂无可查看的子任务" :image-size="60"/>
<article v-for="item in task.workItems||[]" :key="item.id" class="work-item"><div class="row-between"><b>{{ item.title }}</b><span>{{ item.percent===100?'已完成':'进行中' }}</span></div><p class="muted">{{ item.deptName }} · 负责人 {{ item.ownerName }} · 截止 {{ date(item.deadline) }}</p><el-progress :percentage="item.percent"/><div v-if="item.allowedActions?.includes('update')&&(!auth.me?.permissions||auth.me.permissions.includes('TASK_UPDATE'))" class="progress-edit"><span>更新进度</span><el-input-number v-model="percent[String(item.id)]" :min="0" :max="100" :placeholder="String(item.percent)" size="small"/><el-button size="small" :loading="busy" @click="update(item)">保存</el-button></div></article>
<el-dialog v-model="dialog" title="新建子任务" width="560px" class="app-card-dialog" align-center><el-alert v-if="error" :title="error" type="error" :closable="false"/><el-form label-position="top" @submit.prevent="create"><el-form-item label="子任务名称" required><el-input v-model="form.title" maxlength="300"/></el-form-item><el-form-item label="所属部门" required><el-select v-model="form.deptId"><el-option v-for="d in task.workItemDepartments||[]" :key="d.id" :value="d.id" :label="d.name"/></el-select></el-form-item><el-form-item label="负责人" required><el-select v-model="form.ownerId" filterable placeholder="选择部门成员"><el-option v-for="u in members" :key="u.id" :value="u.id" :label="u.name"/></el-select></el-form-item><el-form-item label="截止时间" required><el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" style="width:100%"/></el-form-item><div class="form-footer"><el-button @click="dialog=false">取消</el-button><el-button type="primary" native-type="submit" :loading="busy">创建</el-button></div></el-form></el-dialog>
</section>
</template>
<style scoped>.work-items{margin-top:24px;border-top:1px solid var(--el-border-color);padding-top:20px}.work-item{padding:16px 0;border-bottom:1px solid var(--el-border-color-lighter)}.progress-edit{display:flex;align-items:center;gap:10px;margin-top:12px}</style>
