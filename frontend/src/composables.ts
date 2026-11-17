import { onBeforeUnmount,ref } from 'vue'
import { errorText } from './api'
import { markDataUpdated } from './data-refresh'
export function useLoad(){const loading=ref(false),error=ref('');let sequence=0;async function run<T>(fetcher:()=>Promise<T>,apply:(value:T)=>unknown){const current=++sequence;loading.value=true;error.value='';try{const result=await fetcher();if(sequence===current){if(apply(result)!==false)markDataUpdated()}}catch(e){if(sequence===current)error.value=errorText(e)}finally{if(sequence===current)loading.value=false}}onBeforeUnmount(()=>{sequence++});return {loading,error,run}}
export function usePoll(fn:()=>void,interval=15000){const timer=setInterval(()=>{if(document.visibilityState==='visible')fn()},interval);onBeforeUnmount(()=>clearInterval(timer))}
