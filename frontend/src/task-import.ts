export function parseTaskRows(text:string):{title:string;content:string;sourceRow:number}[]{
 if(text.length>500000)throw new Error('一次最多粘贴约50万字，请拆分导入')
 const rows:string[][]=[];let row:string[]=[],value='',quoted=false
 const source=text.replace(/\r\n/g,'\n')
 for(let i=0;i<=source.length;i++){
  const c=source[i]
  if(c==='"'){if(quoted&&source[i+1]==='"'){value+='"';i++}else if(quoted||value==='')quoted=!quoted;else value+=c}
  else if(!quoted&&(c==='\t'||c==='\n'||c===undefined)){row.push(value.trim());value='';if(c!=='\t'){rows.push(row);row=[]}}
  else if(c!==undefined)value+=c
 }
 if(quoted)throw new Error('表格引号未闭合，请从 Excel 重新复制完整区域')
 const normalize=(s:string)=>s.replace(/\s/g,'')
 const headerIndex=rows.findIndex(r=>r.some(c=>normalize(c)==='工作内容'))
 if(headerIndex<0)throw new Error('请连同“工作内容”等表头一起复制')
 const headers=rows[headerIndex].map(normalize),titleIndex=headers.indexOf('工作内容')
 const result=rows.slice(headerIndex+1).flatMap((r,i)=>{
  const title=r[titleIndex]?.trim();if(!title)return []
  if(title.length>200)throw new Error(`第${headerIndex+i+2}行工作内容较长，请先缩短为200字内的任务标题`)
  const content=r.map((v,j)=>v&&headers[j]&&headers[j]!=='序号'?`${headers[j]}：${v}`:'').filter(Boolean).join('\n\n')
  if(content.length>10000)throw new Error('单条任务内容不能超过10000字')
  return [{title,content,sourceRow:headerIndex+i+2}]
 })
 if(result.length>100)throw new Error('一次最多导入100条事项')
 return result
}
