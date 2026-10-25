import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api, resetSessionBoundary } from '../api'
import type { Me } from '../domain'
export const useAuth=defineStore('auth',()=>{
 const me=ref<Me|null>(null),initialized=ref(false)
 const ready=computed(()=>!!me.value&&!me.value.mustChangePassword&&(!me.value.totpRequired||!!me.value.totpVerified))
 function clearDrafts(){try{for(let i=sessionStorage.length-1;i>=0;i--){const key=sessionStorage.key(i);if(key?.startsWith("task-draft:"))sessionStorage.removeItem(key)}}catch{/* Session storage may be unavailable in privacy mode. */}}
 function clear(){me.value=null;clearDrafts();resetSessionBoundary()}
 async function refresh(){const result=await api.get<Me>('/auth/me');me.value=result;initialized.value=true;return result}
 async function login(payload:unknown){resetSessionBoundary();await api.post('/auth/login',payload);await refresh()}
 async function logout(){try{await api.post('/auth/logout')}finally{clear()}}
 async function switchContext(scopeId:string){clearDrafts();resetSessionBoundary();me.value=null;await api.post('/auth/context',{scopeId});await refresh()}
 return {me,ready,initialized,clear,refresh,login,logout,switchContext}
})
