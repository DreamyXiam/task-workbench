/** Browser fixture smoke only. This does not certify server authorization or live API behavior. */
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import assert from 'node:assert/strict'
const require=createRequire(import.meta.url)
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright')
const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROMIUM||undefined})
const context=await browser.newContext({viewport:{width:1440,height:1000},locale:'zh-CN'})
const page=await context.newPage(),issues=[],writes=[],checks=[]
let loggedIn=false,role='COMPANY_LEADER'
const me=()=>({id:'fixture-user',name:'测试人员',accountSuffix:'0026',role,companyId:'fixture-company',deptId:'fixture-dept',mustChangePassword:false,totpRequired:false,totpVerified:true,scopes:[{id:'leader',role:'COMPANY_LEADER',companyId:'fixture-company'},{id:'admin',role:'ADMIN',companyId:'fixture-company'}]})
const task={id:'fixture-task',code:'RW-2026-001',title:'完成年度重点项目验收准备',content:'明确验收材料、核对交付成果，并按计划完成跨部门资料汇总。',companyId:'fixture-company',companyName:'测试公司',hostDeptId:'fixture-dept',hostDeptName:'综合管理部',priority:'HIGH',status:'IN_PROGRESS',deadline:'2026-12-31T18:00:00+08:00',version:3,allowedActions:['summarize','deadline','urge','comment','upload'],subTasks:[{id:'fixture-sub',deptId:'fixture-dept',deptName:'综合管理部',role:'HOST',ownerName:'测试人员',percent:45,status:'IN_PROGRESS',version:2,allowedActions:['progress','submit','extend','transfer']}],comments:[],progress:[],timeline:[{id:'event1',actionName:'确认分配',operatorName:'测试人员',createdAt:'2026-10-24T10:00:00+08:00',description:'已确认责任部门'}],attachments:[],deadlineRequests:[],collaborationRequests:[]}
const companies=[{id:'fixture-company',name:'测试公司',code:'TEST',enabled:true}]
const departments=[{id:'fixture-dept',name:'综合管理部',companyId:'fixture-company'}]
page.on('pageerror',e=>issues.push(e.message))
await page.route('**/api/v1/**',async route=>{const req=route.request(),url=new URL(req.url()),path=url.pathname.replace('/api/v1',''),method=req.method();let data={};if(method!=='GET'){writes.push({path,headers:req.headers(),body:req.postDataJSON()});if(path==='/auth/login')loggedIn=true;if(path==='/auth/logout')loggedIn=false;if(path==='/auth/context')role=req.postDataJSON().scopeId==='admin'?'ADMIN':'COMPANY_LEADER';if(path.endsWith('/comments'))task.comments.push({id:'comment1',authorName:'测试人员',content:req.postDataJSON().content,createdAt:new Date().toISOString()})}
 if(path==='/auth/csrf')data={token:'fixture-csrf',headerName:'X-CSRF-TOKEN'}
 else if(path==='/auth/me'){if(!loggedIn){await route.fulfill({status:401,json:{code:'UNAUTHENTICATED',message:'请登录',traceId:'fixture-auth'}});return}data=me()}
 else if(path==='/stats/overview')data={total:12,inProgress:7,pendingAcceptance:3,overdue:2,completed:2,recentTasks:[task]}
 else if(path==='/stats/dept')data=[{deptId:'fixture-dept',deptName:'综合管理部',total:12,completed:2,overdue:2,inProgress:7,pendingAcceptance:3,completionRate:16.67}]
 else if(path==='/tasks'||path==='/sub-tasks/mine')data={items:[task],total:1,page:1,pageSize:20}
 else if(path==='/tasks/fixture-task')data=task
 else if(path==='/tasks/match')data={suggestions:[{deptId:'fixture-dept',deptName:'综合管理部',role:'HOST',score:90,reasons:['命中：项目验收']}],ruleVersion:'fixture-v1'}
 else if(path==='/organization/departments'||path==='/organization/collaboration-departments')data=departments
 else if(path==='/organization/delegations'||path==='/organization/appointments'||path==='/organization/handover-tasks'||path==='/organization/handovers')data={items:[]}
 else if(path==='/kpi/calendar-policy')data={calendars:[],templates:[],simulations:[],allowedActions:role==='ADMIN'?['createCalendar','createTemplate']:['simulate']}
 else if(path==='/organization/users')data=[{id:'fixture-user',name:'测试人员'}]
 else if(path==='/kpi/scores')data=[{id:'fixture-score',deptName:'综合管理部',period:'2026-10',score:85.83,templateId:'fixture-template',templateVersion:'1',runId:'fixture-run',status:'OBJECTION',allowedActions:['appeal','adjust'],metrics:[{name:'按时完成率',rawValue:90,score:100,weight:30}],appeals:[]}]
 else if(path==='/kpi/runs')data=[{id:'fixture-run',period:'2026-10',templateId:'fixture-template',simulation:true,status:'SIMULATION'}]
 else if(path==='/kpi/templates')data=[{id:'fixture-template',name:'兼容模板',version:1,weights:[30,25,20,10,15]}]
 else if(path==='/notifications')data={items:[{id:'fixture-note',title:'任务进度提醒',content:'请及时更新项目进度',createdAt:'2026-10-24T10:00:00+08:00',taskId:'fixture-task'}],total:1,page:1,pageSize:20}
 else if(path==='/collaborations')data=[]
 else if(path==='/admin/companies')data=companies
 else if(path==='/admin/departments')data=departments
 else if(path==='/admin/users')data=[{...me(),account:'fixture0026',enabled:true}]
 else if(path.startsWith('/admin/'))data=[]
 await route.fulfill({json:{code:'OK',message:'',data,traceId:'fixture-trace'}})
})
try{
 await page.goto('http://127.0.0.1:5173')
 await page.getByPlaceholder('请输入工号').fill('fixture0026')
 await page.getByPlaceholder('请输入密码').fill('fixture-password')
 assert.equal(await page.locator('.watermark').count(),0,'login has no watermark')
 await page.getByRole('button',{name:'登录工作台',exact:true}).click()
 await page.getByRole('heading',{name:'领导工作概览'}).waitFor()
 assert.equal(await page.locator('.watermark').count(),1,'authenticated view has watermark')
 for(const width of [375,390,768,1024,1440]){
  await page.setViewportSize({width,height:960})
  for(const path of ['/','/tasks','/tasks/new','/tasks/fixture-task','/kpi','/reports','/notifications','/organization-changes','/calendar-simulation']){
   await page.goto('http://127.0.0.1:5173/#'+path)
   await page.locator('.main-content h1').first().waitFor()
   await page.waitForTimeout(200)
   const size=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,width:window.innerWidth}))
   assert.ok(size.scroll<=size.width,`overflow ${width} ${path}: ${JSON.stringify(size)}`)
   checks.push({width,path,overflow:false});if(process.env.QA_SCREENSHOTS&&path==='/'&&[375,1440].includes(width)){await mkdir('tests/results',{recursive:true});await page.screenshot({path:`tests/results/dashboard-${width}.png`,fullPage:true})}
  }
 }
 await page.setViewportSize({width:1440,height:1000})
 await page.goto('http://127.0.0.1:5173/#/tasks/fixture-task')
 await page.getByRole('button',{name:'更新进度',exact:true}).click()
 await page.getByRole('dialog').waitFor()
 await page.getByRole('button',{name:'确认更新进度',exact:true}).click()
 await page.getByRole('dialog').waitFor({state:'hidden'})
 await page.getByPlaceholder('补充说明或同步进展').fill('验证沟通记录')
 await page.getByRole('button',{name:'发送意见',exact:true}).click()
 await page.getByText('验证沟通记录',{exact:true}).waitFor()
 const progressWrite=writes.find(w=>w.path.endsWith('/progress'))
 assert.equal(progressWrite.headers['x-csrf-token'],'fixture-csrf')
 assert.ok(progressWrite.headers['idempotency-key'])
 assert.equal(progressWrite.body.version,2)
 await page.getByRole('button',{name:/公司领导/}).hover()
 await page.getByText('系统管理员 · fixture-company',{exact:true}).click()
 await page.getByRole('heading',{name:'管理工作台'}).waitFor()
 await page.goto('http://127.0.0.1:5173/#/admin/users')
 await page.getByRole('heading',{name:'用户与岗位'}).waitFor()
 await page.getByRole('button',{name:'编辑',exact:true}).click()
 await page.getByText('岗位与组织范围',{exact:true}).waitFor()
 await page.getByRole('button',{name:'返回',exact:true}).click()
 await page.locator('.profile').hover()
 await page.getByText('退出登录',{exact:true}).click()
 await page.getByRole('heading',{name:'登录工作台'}).waitFor()
 assert.equal(await page.locator('.watermark').count(),0,'logout destroys watermark')
 assert.deepEqual(issues,[],'no browser runtime errors')
 await mkdir('tests/results',{recursive:true})
 await writeFile('tests/results/browser-smoke.json',JSON.stringify({mode:'synthetic API fixture only',checks,writes:writes.map(w=>({path:w.path,csrf:!!w.headers['x-csrf-token'],idempotency:!!w.headers['idempotency-key']})),issues},null,2))
 console.log(`PASS fixture smoke: ${checks.length} route/viewport checks, CSRF/idempotency/version action, comment, context switch, admin editor, watermark lifecycle.`)
}catch(e){console.error(JSON.stringify({body:await page.locator("body").innerText(),issues,writes:writes.map(w=>w.path)},null,2));throw e}finally{await browser.close()}
