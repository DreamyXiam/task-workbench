<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from 'lucide-vue-next'

const props=defineProps<{items:{to:string;label:string}[]}>()
const router=useRouter(),open=ref(false),query=ref(''),active=ref(0)
const input=ref<{focus:()=>void}|null>(null)
const filtered=computed(()=>props.items.filter(item=>item.label.toLowerCase().includes(query.value.trim().toLowerCase())))
watch(query,()=>{active.value=0})
function show(){query.value='';active.value=0;open.value=true}
function move(delta:number){if(filtered.value.length)active.value=(active.value+delta+filtered.value.length)%filtered.value.length}
function choose(item:{to:string;label:string}){open.value=false;void router.push(item.to)}
function chooseActive(){const item=filtered.value[active.value];if(item)choose(item)}
function enter(event:KeyboardEvent){if(event.isComposing)return;event.preventDefault();chooseActive()}
function shortcut(event:KeyboardEvent){if((event.ctrlKey||event.metaKey)&&event.key.toLowerCase()==='k'){event.preventDefault();show()}}
onMounted(()=>window.addEventListener('keydown',shortcut))
onBeforeUnmount(()=>window.removeEventListener('keydown',shortcut))
</script>

<template>
  <button type="button" class="command-trigger" aria-label="打开快速导航" @click="show">
    <Search :size="16" aria-hidden="true"/><span>搜索页面</span><kbd>Ctrl K</kbd>
  </button>
  <el-dialog v-model="open" title="快速导航" class="command-dialog" width="520px" append-to-body @opened="nextTick(()=>input?.focus())">
    <el-input ref="input" v-model="query" placeholder="搜索页面" aria-label="搜索页面" clearable @keydown.down.prevent="move(1)" @keydown.up.prevent="move(-1)" @keydown.enter="enter"/>
    <div class="command-list" aria-label="可访问的页面">
      <button v-for="(item,index) in filtered" :key="item.to" type="button" class="command-item" :class="{selected:index===active}" @mouseenter="active=index" @click="choose(item)">{{ item.label }}</button>
      <p v-if="!filtered.length" class="command-empty">没有找到匹配的页面</p>
    </div>
    <p class="command-hint">使用方向键选择，按 Enter 打开</p>
  </el-dialog>
</template>
