<script setup lang="ts">
import { ref } from 'vue'
import { Moon, Sun } from 'lucide-vue-next'
import { applyTheme, initialTheme } from '../theme'

const theme = ref(initialTheme())
function toggle() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  applyTheme(theme.value)
}
</script>

<template>
  <button class="theme-toggle" type="button" :aria-label="theme==='dark'?'切换为浅色模式':'切换为深色模式'" :title="theme==='dark'?'切换为浅色模式':'切换为深色模式'" @click="toggle">
    <Sun v-if="theme==='dark'" :size="17" aria-hidden="true"/>
    <Moon v-else :size="17" aria-hidden="true"/>
  </button>
</template>
