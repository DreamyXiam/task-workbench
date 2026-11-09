import type {Row} from './domain'
export const departmentReportOptions=[
 {id:'progress',name:'推进情况',type:'TEXT',required:false},
 {id:'problem',name:'存在问题',type:'TEXT',required:false},
 {id:'suggestion',name:'解决建议',type:'TEXT',required:false},
 {id:'coordination',name:'需要领导协调解决的工作',type:'TEXT',required:false},
 {id:'timeRequirement',name:'时间要求',type:'TEXT',required:false},
]
export const selectedReportFields=(ids:string[])=>departmentReportOptions.filter(f=>ids.includes(f.id)).map(f=>({...f}))
export function composeDepartmentReport(fields:Row[],values:Row):string{
 return fields.filter(f=>values[f.id]!=null&&String(values[f.id]).trim()!=='').map(f=>`${f.name}：${String(values[f.id]).trim()}`).join('\n')
}
