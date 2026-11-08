<script setup lang="ts">
import type { Row } from '../domain'
defineProps<{ values?:Row }>()
const fields=defineModel<Row[]>('fields',{required:true})
let dragged=-1
function add(){fields.value=[...fields.value,{id:crypto.randomUUID(),name:'',type:'TEXT',required:false}]}
function move(from:number,to:number){if(from<0||to<0||to>=fields.value.length||from===to)return;const copy=[...fields.value];const item=copy.splice(from,1)[0];copy.splice(to,0,item);fields.value=copy}
function remove(index:number){if(window.confirm('移除此字段？已填写的内容将保留在历史记录中。'))fields.value=fields.value.filter((_,i)=>i!==index)}
function options(field:Row,value:string){field.options=value.split('\n').map(s=>s.trim()).filter(Boolean)}
</script>
<template><div class="field-designer"><p class="muted">拖动字段调整顺序，也可使用上移、下移按钮。</p><div v-for="(field,index) in fields" :key="field.id" class="field-card" draggable="true" @dragstart="dragged=index" @dragover.prevent @drop.prevent="move(dragged,index);dragged=-1"><div class="field-row"><span aria-hidden="true">⋮⋮</span><el-input v-model="field.name" maxlength="80" placeholder="字段名称" aria-label="字段名称"/><el-select v-model="field.type" :disabled="values?.[field.id]!==undefined" aria-label="字段类型"><el-option label="文字" value="TEXT"/><el-option label="数字" value="NUMBER"/><el-option label="日期" value="DATE"/><el-option label="下拉选项" value="SELECT"/></el-select><el-checkbox v-model="field.required">必填</el-checkbox></div><el-input v-if="field.type==='SELECT'" type="textarea" :model-value="(field.options||[]).join('\n')" placeholder="每行一个下拉选项" aria-label="下拉选项" @change="(v:string)=>options(field,v)"/><div class="field-actions"><el-button size="small" :disabled="index===0" @click="move(index,index-1)">上移</el-button><el-button size="small" :disabled="index===fields.length-1" @click="move(index,index+1)">下移</el-button><el-button size="small" text type="danger" @click="remove(index)">删除</el-button></div></div><el-button :disabled="fields.length>=30" @click="add">添加字段</el-button></div></template>
<style scoped>.field-card{padding:12px;border:1px solid var(--el-border-color);border-radius:8px;margin:10px 0}.field-row{display:flex;align-items:center;gap:8px}.field-row .el-select{width:130px;flex-shrink:0}.field-actions{margin-top:8px}@media(max-width:600px){.field-row{flex-wrap:wrap}.field-row .el-input{flex-basis:80%}}</style>
