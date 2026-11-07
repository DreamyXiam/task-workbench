<script setup lang="ts">
import { computed,onMounted,ref } from 'vue'
import { api,ApiError,errorText } from '../api'
import { roles } from '../domain'
import type { Row } from '../domain'
import { ElMessage } from 'element-plus'
const rows=ref<Row[]>([]),catalog=ref<Record<string,string>>({}),selected=ref(''),disabled=ref<string[]>([]),busy=ref(false),loading=ref(false),error=ref(''),needsTotp=ref(false),code=ref('')
const row=computed(()=>rows.value.find(r=>r.role===selected.value))
async function load(){loading.value=true;error.value='';try{const data=await api.get('/admin/role-permissions');catalog.value=data.catalog;rows.value=data.roles;if(!selected.value)selected.value=rows.value[0]?.role??'';choose(selected.value)}catch(e){error.value=errorText(e)}finally{loading.value=false}}
function choose(role:string){selected.value=role;disabled.value=[...(rows.value.find(r=>r.role===role)?.disabled??[])];needsTotp.value=false;code.value='';error.value=''}
function toggle(key:string,value:unknown){if(value)disabled.value=disabled.value.filter(p=>p!==key);else if(!disabled.value.includes(key))disabled.value=[...disabled.value,key]}
async function save(){if(!row.value||busy.value)return;busy.value=true;error.value='';try{await api.put(`/admin/role-permissions/${selected.value}`,{version:row.value.version,disabled:disabled.value});ElMessage.success('权限配置已保存，相关用户需重新登录');await load()}catch(e){error.value=errorText(e);needsTotp.value=e instanceof ApiError&&e.code==='FRESH_TOTP_REQUIRED'}finally{busy.value=false}}
async function verify(){if(!/^\d{6}$/.test(code.value)){error.value='请输入六位动态验证码';return}busy.value=true;try{await api.post('/auth/totp/verify',{code:code.value});code.value='';needsTotp.value=false;error.value='';ElMessage.success('验证成功，请重新保存')}catch(e){error.value=errorText(e)}finally{busy.value=false}}
onMounted(load)
</script>
<template>
<section class="page-heading"><div><p class="eyebrow">ROLE PERMISSIONS</p><h1>角色权限配置</h1><p class="muted">为现有角色启用或关闭功能，并在“用户与岗位”中分配角色。</p></div></section>
<el-alert title="权限开关只收紧现有角色权限；公司、部门、数据范围和业务状态仍由服务端校验。系统管理员的设置只作用于公司范围管理员；全局管理员权限固定。" type="info" :closable="false" show-icon/>
<el-alert v-if="error" :title="error" type="error" :closable="false"/>
<section v-loading="loading" class="panel"><el-form label-position="top"><el-form-item label="选择角色"><el-select :model-value="selected" @change="choose"><el-option v-for="r in rows" :key="r.role" :label="roles[r.role]||r.role" :value="r.role"/></el-select></el-form-item><p class="muted">取消勾选将禁止该角色使用对应功能；勾选不会赋予角色原本没有的能力。关闭“查看任务”时，任务办理、附件、协同、统计、通知和交接也将不可用。保存后该角色用户需要重新登录。</p><div class="permissions-grid"><el-checkbox v-for="(label,key) in catalog" :key="key" :model-value="!disabled.includes(String(key))" @change="toggle(String(key),$event)">{{ label }}</el-checkbox></div><div v-if="needsTotp" class="verification"><p>请输入验证器中的六位动态验证码，再重新保存。</p><el-input v-model="code" maxlength="6" inputmode="numeric" placeholder="动态验证码"/><el-button :loading="busy" @click="verify">验证</el-button></div><div class="form-footer"><el-button type="primary" :loading="busy" :disabled="!row||needsTotp" @click="save">保存权限</el-button></div></el-form></section>
</template>
<style scoped>.permissions-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:12px;margin:20px 0}.verification{max-width:360px;margin:16px 0}.verification .el-button{margin-top:8px}</style>
