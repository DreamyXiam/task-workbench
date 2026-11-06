<script setup lang="ts">
defineProps<{loading?:boolean;error?:string;empty?:boolean}>();defineEmits<{retry:[]}>()
</script>
<template>
  <div v-if="loading" class="page-skeleton" role="status" aria-live="polite" aria-label="正在加载工作数据">
    <span class="sr-only">正在加载工作数据…</span>
    <div class="skeleton-line short"/><div class="skeleton-line medium"/>
    <div class="skeleton-grid"><div/><div/><div/><div/></div>
    <div class="skeleton-block"/>
  </div>
  <div v-else-if="error" class="error-state" role="alert"><strong>暂时无法加载</strong><p>{{ error }}</p><el-button @click="$emit('retry')">重新加载</el-button></div>
  <el-empty v-else-if="empty" description="当前范围内暂无记录"/>
  <slot v-else/>
</template>
