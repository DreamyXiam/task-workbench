<script setup lang="ts">
import { computed,onMounted,onBeforeUnmount,ref,watch } from 'vue'
import { useAuth } from '../stores/auth'
const auth=useAuth(),background=ref('')
const identity=computed(()=>auth.me?`${auth.me.name} · ${auth.me.accountSuffix}`:'')
let timer:ReturnType<typeof setInterval>|undefined
function draw(){ if(!auth.ready){background.value='';return}const canvas=document.createElement('canvas');canvas.width=420;canvas.height=220;const ctx=canvas.getContext('2d');if(!ctx)return;ctx.translate(210,110);ctx.rotate(-Math.PI/7);ctx.fillStyle='rgba(65,82,103,0.10)';ctx.textAlign='center';ctx.font='14px Microsoft YaHei, sans-serif';ctx.fillText(identity.value,-0,-22);ctx.fillText(new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',dateStyle:'short'}).format(new Date()),0,2);ctx.fillText('内部数据 请勿外传',0,26);background.value=`url(${canvas.toDataURL()})`}
watch([identity,()=>auth.ready],draw)
onMounted(()=>{draw();timer=setInterval(draw,60000)})
onBeforeUnmount(()=>{clearInterval(timer);background.value=''})
</script>
<template><div v-if="auth.ready" class="watermark" aria-hidden="true" :style="{backgroundImage:background}" /></template>
