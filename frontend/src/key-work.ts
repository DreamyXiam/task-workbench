import type { Row } from './domain'
export function keyWorkFields(){return [
 {id:'progress',name:'推进情况',type:'TEXT',required:false},
 {id:'problem',name:'存在问题',type:'TEXT',required:false},
 {id:'suggestion',name:'解决建议',type:'TEXT',required:false},
 {id:'coordination',name:'需要领导协调解决的工作',type:'TEXT',required:false},
 {id:'remark',name:'备注',type:'TEXT',required:false},
]}
export function summaryValue(data:Row|undefined,id:string){
 const expected=keyWorkFields().find(field=>field.id===id)
 const field=data?.fields?.find((entry:{id:string;name:string})=>entry.id===id)
 if(!expected||!field||field.name!==expected.name)return '—'
 return data?.values?.[id]??'—'
}
