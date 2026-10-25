<script setup lang="ts">
defineProps<{loading?:boolean;error?:string;empty?:boolean}>();defineEmits<{retry:[]}>()
</script>
<template><div v-if="loading" class="loading" role="status" aria-live="polite"><span class="spinner"/>正在加载工作数据…</div><div v-else-if="error" class="error-state" role="alert"><strong>暂时无法加载</strong><p>{{ error }}</p><el-button @click="$emit('retry')">重新加载</el-button></div><el-empty v-else-if="empty" description="当前范围内暂无记录"/><slot v-else/></template>
