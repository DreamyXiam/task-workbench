import { onBeforeUnmount,ref } from 'vue'
import { errorText } from './api'
export function useLoad(){const loading=ref(false),error=ref('');let sequence=0;async function run<T>(fetcher:()=>Promise<T>,apply:(value:T)=>void){const current=++sequence;loading.value=true;error.value='';try{const result=await fetcher();if(sequence===current)apply(result)}catch(e){if(sequence===current)error.value=errorText(e)}finally{if(sequence===current)loading.value=false}}onBeforeUnmount(()=>{sequence++});return {loading,error,run}}
export function usePoll(fn:()=>void,interval=30000){const timer=setInterval(()=>{if(document.visibilityState==='visible')fn()},interval);onBeforeUnmount(()=>clearInterval(timer))}
