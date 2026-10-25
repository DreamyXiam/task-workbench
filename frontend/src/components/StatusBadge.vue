<script setup lang="ts">
import { statuses } from '../domain'
defineProps<{status?:string;overdue?:boolean}>()
</script>
<template><span class="status" :class="[status?.toLowerCase(),{late:overdue}]"><span class="status-dot"/>{{ statuses[status||'']||status||'未标注' }}</span><span v-if="overdue" class="late-flag">曾逾期 / 当前逾期</span></template>
