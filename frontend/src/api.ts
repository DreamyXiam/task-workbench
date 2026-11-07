import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
export class ApiError extends Error { constructor(public status:number,public code:string,message:string,public traceId?:string){super(message);this.name='ApiError'} }
export const transport=axios.create({baseURL:'/api/v1',withCredentials:true,timeout:30000,validateStatus:()=>true})
let epoch=0
let csrf:{token:string;headerName:string}|null=null
let csrfPromise:Promise<{token:string;headerName:string}>|null=null
let expire=()=>{}
export const onSessionExpired=(callback:()=>void)=>{expire=callback}
export const sessionEpoch=()=>epoch
export const resetSessionBoundary=()=>{epoch++;csrf=null;csrfPromise=null}
async function csrfToken(){ if(csrf)return csrf; if(!csrfPromise){const started=epoch;csrfPromise=transport.get('/auth/csrf').then(res=>{if(epoch!==started)throw new ApiError(0,'STALE_SESSION','身份已切换，请重试');if(res.status!==200||res.data.code!=='OK')throw new ApiError(res.status,res.data.code??'CSRF_FAILED',res.data.message??'无法获取安全令牌',res.data.traceId); csrf=res.data.data;return csrf!}).finally(()=>{csrfPromise=null})}return csrfPromise }
async function request<T=any>(path:string,method:string,data?:unknown,config:AxiosRequestConfig={}):Promise<T>{
 const started=epoch
 const headers:Record<string,string>={}
 if(!['GET','HEAD'].includes(method)){const token=await csrfToken();headers[token.headerName]=token.token;if(!path.startsWith('/auth/'))headers['Idempotency-Key']=crypto.randomUUID()}
 if(epoch!==started)throw new ApiError(0,'STALE_SESSION','身份已切换，请重试')
 let res
 try{res=await transport.request({...config,url:path,method,data,headers:{...headers,...config.headers}})}catch(error){if(axios.isAxiosError(error))throw new ApiError(0,'NETWORK_ERROR','网络连接失败，请检查网络后重试');throw error}
 if(epoch!==started)throw new ApiError(0,'STALE_SESSION','身份已切换，请重新加载')
 if(res.status<200||res.status>=300||(!config.responseType&&res.data?.code!=='OK')){
  let body=res.data??{};if(typeof Blob!=='undefined'&&body instanceof Blob){try{body=JSON.parse(await body.text())}catch{body={}}}  if(res.status===401){resetSessionBoundary();expire()} if(body.code?.includes('CSRF'))csrf=null
  throw new ApiError(res.status,body.code??'REQUEST_FAILED',body.message??`请求失败（${res.status}）`,body.traceId)
 }
 if(path==='/auth/login'||path==='/auth/change-password'){csrf=null;csrfPromise=null}
 return config.responseType?res.data:res.data.data
}
export const api={get:<T=any>(path:string,params?:Record<string,unknown>)=>request<T>(path,'GET',undefined,{params}),post:<T=any>(path:string,data:unknown={},config:AxiosRequestConfig={})=>request<T>(path,'POST',data,config),put:<T=any>(path:string,data:unknown)=>request<T>(path,'PUT',data),delete:<T=any>(path:string,data:unknown={})=>request<T>(path,'DELETE',data),upload:(data:FormData)=>request('/attachments','POST',data),download:async(path:string,filename:string)=>{const blob=await request<Blob>(path,'GET',undefined,{responseType:'blob'});const href=URL.createObjectURL(blob);const a=document.createElement('a');a.href=href;a.download=filename;a.click();setTimeout(()=>URL.revokeObjectURL(href),30000)},preview:async(path:string)=>{const win=window.open('about:blank','_blank');if(!win)throw new ApiError(0,'POPUP_BLOCKED','浏览器阻止了预览窗口，请允许弹窗或下载查看');win.opener=null;try{const blob=await request<Blob>(path,'GET',undefined,{responseType:'blob'});const href=URL.createObjectURL(blob);win.location.href=href;setTimeout(()=>URL.revokeObjectURL(href),60000)}catch(e){win.close();throw e}}}
export function errorText(error:unknown){return error instanceof ApiError?`${error.message}${error.status===409?'；数据已变化，请刷新后重新操作。':''}${error.traceId?`（追踪号 ${error.traceId}）`:''}`:error instanceof Error?error.message:'操作失败，请重试'}
