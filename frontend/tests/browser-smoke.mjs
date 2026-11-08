/** Browser fixture smoke only. This does not certify server authorization or live API behavior. */
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import assert from 'node:assert/strict'
const require=createRequire(import.meta.url)
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright')
const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROMIUM||undefined})
const context=await browser.newContext({viewport:{width:1440,height:1000},locale:'zh-CN',colorScheme:'light'})
const page=await context.newPage(),issues=[],writes=[],checks=[]
let loggedIn=false,role='COMPANY_LEADER',expireTasks=false,adminReauthRequired=false,adminReauthVerified=false,adminCompanySaves=0,adminGlobal=false
const me=()=>({id:'fixture-user',name:'测试人员',accountSuffix:'0026',role,companyId:role==='GROUP_LEADER'||adminGlobal?null:'fixture-company',deptId:role==='GROUP_LEADER'?null:'fixture-dept',mustChangePassword:false,totpRequired:false,totpVerified:true,scopes:[{id:'leader',role:'COMPANY_LEADER',companyId:'fixture-company',companyName:'测试公司'},{id:'admin',role:'ADMIN',companyId:'fixture-company',companyName:'测试公司'}]})
const task={id:'fixture-task',code:'RW-2026-001',title:'完成年度重点项目验收准备',content:'明确验收材料、核对交付成果，并按计划完成跨部门资料汇总。',companyId:'fixture-company',companyName:'测试公司',hostDeptId:'fixture-dept',hostDeptName:'综合管理部',priority:'HIGH',status:'IN_PROGRESS',deadline:'2026-12-31T18:00:00+08:00',version:3,allowedActions:['summarize','deadline','urge','comment','upload'],subTasks:[{id:'fixture-sub',deptId:'fixture-dept',deptName:'综合管理部',role:'HOST',ownerName:'测试人员',percent:45,status:'IN_PROGRESS',version:2,allowedActions:['progress','submit','extend','transfer']}],comments:[],progress:[],timeline:[{id:'event1',actionName:'确认分配',operatorName:'测试人员',createdAt:'2026-10-24T10:00:00+08:00',description:'已确认责任部门'}],attachments:[],deadlineRequests:[],collaborationRequests:[]}
task.latestProgress='卡片展示的最新执行记录';task.progressSource='部门进度';task.reportedPercent=45;task.progressAt='2026-11-08T03:00:00Z';task.progressDeptName='综合管理部'
task.createdAt='2026-10-24T10:00:00+08:00'
const companies=[{id:'fixture-company',name:'测试公司',companyCode:'TEST',enabled:true}]
const departments=[{id:'fixture-dept',name:'综合管理部',companyId:'fixture-company',companyName:'测试公司'},{id:'fixture-other-dept',name:'第二部门',companyId:'fixture-other-company',companyName:'第二公司'}]
page.on('pageerror',e=>issues.push(e.message))
await page.route('**/api/v1/**',async route=>{const req=route.request(),url=new URL(req.url()),path=url.pathname.replace('/api/v1',''),method=req.method();let data={};if(method!=='GET'){writes.push({path,headers:req.headers(),body:req.postDataJSON()});if(path==='/auth/login')loggedIn=true;if(path==='/auth/logout')loggedIn=false;if(path==='/auth/context')role=req.postDataJSON().scopeId==='admin'?'ADMIN':'COMPANY_LEADER';if(path.endsWith('/comments'))task.comments.push({id:'comment1',authorName:'测试人员',content:req.postDataJSON().content,createdAt:new Date().toISOString()})}
 if(path==='/admin/companies'&&method==='POST'&&adminReauthRequired){if(!adminReauthVerified){await route.fulfill({status:403,json:{code:'FRESH_TOTP_REQUIRED',message:'请在五分钟内完成二次验证',traceId:'fixture'}});return}adminCompanySaves++;companies.push({id:'fixture-new-company',name:'新建关联公司',companyCode:'FIXTURE',enabled:true})}
 if(path==='/auth/totp/verify'&&method==='POST'&&adminReauthRequired){if(req.postDataJSON().code!=='123456'){await route.fulfill({status:422,json:{code:'INVALID_TOTP',message:'验证码无效',traceId:'fixture'}});return}adminReauthVerified=true}
 if(expireTasks&&path==='/tasks'){loggedIn=false;await route.fulfill({status:401,json:{code:'UNAUTHENTICATED',message:'请登录',traceId:'fixture-expired'}});return}
 if(path==='/auth/csrf')data={token:'fixture-csrf',headerName:'X-CSRF-TOKEN'}
 else if(path==='/auth/me'){if(!loggedIn){await route.fulfill({status:401,json:{code:'UNAUTHENTICATED',message:'请登录',traceId:'fixture-auth'}});return}data=me()}
 else if(path==='/stats/overview')data={total:12,inProgress:7,pendingAcceptance:3,overdue:2,completed:2,recentTasks:[task]}
 else if(path==='/stats/dept')data=[{deptId:'fixture-dept',deptName:'综合管理部',total:12,completed:2,overdue:2,inProgress:7,pendingAcceptance:3,completionRate:16.67}]
 else if(path==='/tasks'&&method==='POST')data={id:'batch-created-'+writes.length,status:'DRAFT'}
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
 else if(path==='/admin/users')data=[{...me(),enabled:true,scopes:[{role:'ADMIN',companyId:'fixture-company',enabled:true},{role:'ADMIN',companyId:'fixture-company',enabled:false}]}]
 else if(path.startsWith('/admin/'))data=[]
 await route.fulfill({json:{code:'OK',message:'',data,traceId:'fixture-trace'}})
})
try{
 await page.goto('http://127.0.0.1:5173')
 await page.getByRole('heading',{name:'登录工作台',exact:true}).waitFor()
 assert.equal(await page.locator('footer.source-notice').count(),0,'source link is not in a footer')
 assert.equal(await page.getByRole('link',{name:'开源许可与源码'}).count(),1,'source remains accessible before sign in')
 assert.equal(await page.getByText('会话已失效，请重新登录').count(),0,'first visit must not report an expired session')
 await page.getByPlaceholder('请输入工号').fill('fixture0026')
 await page.getByPlaceholder('请输入密码').fill('fixture-password')
 assert.equal(await page.locator('.watermark').count(),0,'login has no watermark')
 await page.getByRole('button',{name:'登录工作台',exact:true}).click()
 await page.getByRole('heading',{name:'领导工作概览'}).waitFor()
 assert.equal(await page.locator('.watermark').count(),1,'authenticated view has watermark')
 await page.locator('.profile').hover()
 await page.getByRole('link',{name:'开源许可与源码'}).waitFor()
 await page.mouse.move(0,0)
 await page.getByRole('button',{name:'切换为深色模式'}).click()
 assert.equal(await page.evaluate(()=>document.documentElement.dataset.theme),'dark','dark theme applies')
 await page.reload()
 await page.getByRole('heading',{name:'领导工作概览'}).waitFor()
 assert.equal(await page.evaluate(()=>document.documentElement.dataset.theme),'dark','theme persists after reload')
 await page.getByRole('button',{name:'切换为浅色模式'}).click()
 await page.getByRole('button',{name:'收起侧边栏'}).click()
 assert.equal(await page.locator('.app-layout.sidebar-collapsed').count(),1,'sidebar collapses')
 await page.reload()
 await page.getByRole('heading',{name:'领导工作概览'}).waitFor()
 assert.equal(await page.locator('.app-layout.sidebar-collapsed').count(),1,'sidebar preference persists')
 await page.getByRole('button',{name:'展开侧边栏'}).click()
 await page.keyboard.press('Control+k')
 const command=page.getByRole('dialog',{name:'快速导航'})
 await command.getByPlaceholder('搜索页面').fill('任务中心')
 await command.getByRole('button',{name:'任务中心',exact:true}).click()
 await page.waitForURL('**/#/tasks')
 await command.waitFor({state:'hidden'})
 await page.locator('.task-card').first().waitFor()
 assert.equal(await page.locator('.desktop-table').count(),0,'task center uses cards instead of the table')
 assert.ok((await page.locator('.task-card').first().innerText()).includes('卡片展示的最新执行记录'),'latest progress is visible without opening detail')
 await page.locator('.task-card').first().click({position:{x:8,y:8}})
 await page.waitForURL('**/#/tasks/fixture-task')
 await page.goto('http://127.0.0.1:5173/#/tasks')
 await page.locator('.task-card').first().focus()
 await page.keyboard.press('Enter')
 await page.waitForURL('**/#/tasks/fixture-task')
 await page.goto('http://127.0.0.1:5173/#/tasks')
 await page.getByRole('button',{name:'甘特图',exact:true}).click()
 await page.locator('.time-bar').click()
 await page.waitForURL('**/#/tasks/fixture-task')
 await page.goto('http://127.0.0.1:5173/#/tasks')
 for(const theme of ['light','dark']){
  if(theme==='dark')await page.getByRole('button',{name:'切换为深色模式'}).click()
 for(const width of [375,390,768,1024,1440]){
  await page.setViewportSize({width,height:960})
  for(const path of ['/','/tasks','/tasks/new','/tasks/fixture-task','/kpi','/reports','/notifications','/organization-changes','/calendar-simulation']){
   await page.goto('http://127.0.0.1:5173/#'+path)
   await page.locator('.main-content h1').first().waitFor()
   await page.waitForTimeout(200)
   const size=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,width:window.innerWidth}))
   assert.ok(size.scroll<=size.width,`overflow ${width} ${path}: ${JSON.stringify(size)}`)
   checks.push({theme,width,path,overflow:false});if(process.env.QA_SCREENSHOTS&&path==='/'&&[375,1440].includes(width)){await mkdir('tests/results',{recursive:true});await page.screenshot({path:`tests/results/dashboard-${theme}-${width}.png`,fullPage:true})}
   if(path==='/tasks'){
    await page.getByRole('button',{name:'甘特图',exact:true}).click()
    await page.locator('.time-bar').waitFor()
    assert.ok((await page.locator('.gantt-row').innerText()).includes(task.latestProgress))
    assert.ok((await page.locator('.gantt-row').innerText()).includes('45%'))
    assert.equal(await page.locator('.time-bar').getAttribute('href'),'#/tasks/fixture-task')
    const ganttSize=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,width:window.innerWidth}))
    assert.ok(ganttSize.scroll<=ganttSize.width,`gantt overflow ${theme} ${width}`)
    await page.locator('.gantt-scroll').evaluate(el=>{el.scrollLeft=el.scrollWidth})
    assert.ok(await page.locator('.gantt-scroll').evaluate(el=>el.scrollWidth<=el.clientWidth||el.scrollLeft>0),'timeline scrolls inside its container')
    if(process.env.QA_SCREENSHOTS&&[375,1440].includes(width)){await mkdir('tests/results',{recursive:true});await page.screenshot({path:`tests/results/gantt-${theme}-${width}.png`,fullPage:true})}
    await page.getByRole('button',{name:'卡片',exact:true}).click()
    await page.locator('.task-card').waitFor()
    checks.push({theme,width,path:'/tasks:gantt',overflow:false})
   }
  }
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
 await page.getByText('系统管理员 · 测试公司',{exact:true}).click()
 await page.getByRole('heading',{name:'管理工作台'}).waitFor()
 await page.getByRole('button',{name:'切换为浅色模式'}).click()
 for(const theme of ['light','dark']){
  if(theme==='dark')await page.getByRole('button',{name:'切换为深色模式'}).click()
  for(const width of [375,768,1440])for(const resource of ['companies','departments','users','tags','rules','notification-channels','audit-logs']){
   await page.setViewportSize({width,height:960})
   await page.goto('http://127.0.0.1:5173/#/admin/'+resource)
   await page.locator('.main-content h1').first().waitFor()
   const size=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,width:window.innerWidth}))
   assert.ok(size.scroll<=size.width,`admin overflow ${theme} ${width} ${resource}: ${JSON.stringify(size)}`)
   checks.push({theme,width,path:'/admin/'+resource,overflow:false})
  }
 }
 await page.setViewportSize({width:1440,height:1000})
 await page.goto('http://127.0.0.1:5173/#/admin/users')
 await page.getByRole('heading',{name:'用户与岗位'}).waitFor()
 await page.getByRole('button',{name:'编辑',exact:true}).click()
 await page.getByText('岗位与组织范围',{exact:true}).waitFor()
 const userDrawer=page.getByRole('dialog',{name:'编辑用户与岗位'})
 const accountInput=userDrawer.locator('.el-form-item').filter({hasText:'工号'}).locator('input')
 assert.equal(await accountInput.isDisabled(),true,'existing account is read-only')
 assert.ok((await accountInput.inputValue()).endsWith('0026'),'masked account suffix is shown')
 assert.equal(await userDrawer.locator('.scope-editor').count(),1,'disabled historical scopes are not reactivated')
 await userDrawer.getByRole('button',{name:'保存配置'}).click()
 await userDrawer.waitFor({state:'hidden'})
 const userWrite=writes.find(w=>w.path==='/admin/users/fixture-user')
 assert.ok(userWrite,'existing user edit can be saved without full account')
 assert.equal(userWrite.body.scopes,undefined,'unchanged scopes and appointment metadata are preserved')
 assert.equal(userWrite.body.account,undefined,'masked account is not submitted')
 adminReauthRequired=true
 await page.goto('http://127.0.0.1:5173/#/admin/companies')
 await page.getByRole('button',{name:'＋ 新增公司'}).click()
 const adminDrawer=page.getByRole('dialog',{name:'新增公司管理'})
 const card=adminDrawer.locator('.app-card-dialog')
 assert.equal(await card.count(),1,'admin form uses a centered dialog')
 const dialogBox=await card.boundingBox()
 assert.ok(Math.abs(dialogBox.x+dialogBox.width/2-720)<3,'admin dialog is centered')
 const companyCode=adminDrawer.locator('.el-form-item').filter({hasText:'公司编码'}).locator('input')
 const companyName=adminDrawer.locator('.el-form-item').filter({hasText:'公司名称'}).locator('input')
 await companyCode.fill('FIXTURE')
 await companyName.fill('测试公司')
 await adminDrawer.getByRole('button',{name:'保存配置'}).click()
 await adminDrawer.getByText('请在五分钟内完成二次验证',{exact:false}).waitFor()
 await adminDrawer.getByLabel('动态验证码').fill('000000')
 await adminDrawer.getByRole('button',{name:'验证并返回'}).click()
 await adminDrawer.getByText('验证码无效',{exact:false}).waitFor()
 await adminDrawer.getByLabel('动态验证码').fill('123456')
 await adminDrawer.getByRole('button',{name:'验证并返回'}).click()
 assert.equal(await companyCode.inputValue(),'FIXTURE','reauth preserves company code')
 assert.equal(await companyName.inputValue(),'测试公司','reauth preserves company name')
 assert.equal(adminCompanySaves,0,'reauth must not submit the form automatically')
 await adminDrawer.getByRole('button',{name:'保存配置'}).click()
 await adminDrawer.waitFor({state:'hidden'})
 assert.equal(adminCompanySaves,1,'explicit retry saves once')
 await page.getByText('FIXTURE',{exact:true}).waitFor()
 await page.getByRole('row').filter({hasText:'新建关联公司'}).getByRole('button',{name:'编辑'}).click()
 const editCompany=page.getByRole('dialog',{name:'编辑公司管理'})
 const existingCode=editCompany.locator('.el-form-item').filter({hasText:'公司编码'}).locator('input')
 assert.equal(await existingCode.inputValue(),'FIXTURE','existing company code is displayed')
 assert.equal(await existingCode.isDisabled(),true,'unsupported code editing is disabled')
 await editCompany.getByRole('button',{name:'返回',exact:true}).click()
 await page.goto('http://127.0.0.1:5173/#/admin/departments')
 await page.getByRole('button',{name:'＋ 新增部门'}).click()
 const departmentDrawer=page.getByRole('dialog',{name:'新增部门管理'})
 await departmentDrawer.locator('.el-form-item').filter({hasText:'所属公司'}).locator('.el-select').click()
 await page.getByText('新建关联公司',{exact:true}).waitFor()
 await departmentDrawer.getByRole('button',{name:'返回',exact:true}).click()
 adminReauthRequired=false
 adminGlobal=true
 await page.reload()
 await page.locator('.app-layout').waitFor()
 await page.goto('http://127.0.0.1:5173/#/admin/users')
 await page.getByRole('button',{name:'＋ 新增用户与岗位'}).click()
 const newUser=page.getByRole('dialog',{name:'新增用户与岗位'})
 await newUser.locator('.el-form-item').filter({hasText:'工号'}).locator('input').fill('global-fixture')
 await newUser.locator('.el-form-item').filter({hasText:'姓名'}).locator('input').fill('测试管理员')
 await newUser.locator('.el-form-item').filter({hasText:'初始 / 重置密码'}).locator('input').fill('Fixture-Password123')
 await newUser.getByRole('button',{name:'＋ 添加岗位'}).click()
 const scope=newUser.locator('.scope-editor').first()
 await scope.locator('.el-form-item').filter({hasText:'角色'}).locator('.el-select').click()
 await page.getByRole('option',{name:'系统管理员',exact:true}).click()
 await scope.locator('.el-form-item').filter({hasText:'公司范围'}).locator('.el-select').click()
 await page.getByRole('option',{name:'全局（不限定公司）',exact:true}).click()
 assert.equal(await scope.locator('.el-form-item').filter({hasText:'部门'}).locator('input').isDisabled(),true,'global scope has no department')
 await newUser.getByRole('button',{name:'保存配置'}).click()
 await newUser.waitFor({state:'hidden'})
 const globalWrite=writes.findLast(w=>w.path==='/admin/users')
 assert.equal(globalWrite.body.scopes[0].role,'ADMIN')
 assert.equal(globalWrite.body.scopes[0].companyId,null,'global scope uses null company')
 assert.equal(globalWrite.body.scopes[0].deptId,null,'global scope uses null department')
 adminGlobal=false
 expireTasks=true
 await page.goto('http://127.0.0.1:5173/#/tasks')
 await page.getByRole('heading',{name:'登录工作台',exact:true}).waitFor()
 await page.getByText('会话已失效，请重新登录').waitFor()
 expireTasks=false
 await page.getByPlaceholder('请输入工号').fill('fixture0026')
 await page.getByPlaceholder('请输入密码').fill('fixture-password')
 await page.getByRole('button',{name:'登录工作台',exact:true}).click()
 await page.getByRole('heading',{name:'管理工作台'}).waitFor()
 role='GROUP_LEADER'
 await page.reload()
 await page.locator('.app-layout').waitFor()
 await page.goto('http://127.0.0.1:5173/#/tasks/new')
 const companyPicker=page.locator('.el-form-item').filter({hasText:'任务所属公司'}).locator('.el-select')
 const hostPicker=page.locator('.el-form-item').filter({hasText:'主办部门'}).locator('.el-select')
 await companyPicker.click()
 await page.getByRole('option',{name:'测试公司',exact:true}).click()
 await hostPicker.click()
 await page.getByRole('option',{name:'测试公司 / 综合管理部',exact:true}).click()
 await companyPicker.click()
 await page.getByRole('option',{name:'第二公司',exact:true}).click()
 await hostPicker.click()
 await page.getByRole('option',{name:'第二公司 / 第二部门',exact:true}).waitFor()
 assert.equal(await page.getByRole('option',{name:'测试公司 / 综合管理部',exact:true}).count(),0,'host options are limited to selected company')
 await page.keyboard.press('Escape')
 await page.goto('http://127.0.0.1:5173/#/tasks/import')
 await page.getByRole('heading',{name:'批量布置任务'}).waitFor()
 await page.locator('.el-form-item').filter({hasText:'所属公司'}).locator('.el-select').click()
 await page.getByRole('option',{name:'测试公司',exact:true}).click()
 await page.locator('.el-form-item').filter({hasText:'默认主办部门'}).locator('.el-select').click()
 await page.getByRole('option',{name:'综合管理部',exact:true}).click()
 const batchDeadline=page.locator('.el-form-item').filter({hasText:'本批截止时间'}).locator('input')
 await batchDeadline.fill('2026-12-31 18:00:00')
 await batchDeadline.press('Enter')
 await page.getByPlaceholder('在 Excel 中选中表头与数据行，复制后粘贴到这里。须包含工作内容列。').fill('序号\t工作内容\t推进情况\n1\t批量测试事项\t待推进')
 await page.getByRole('button',{name:'解析并预览'}).click()
 await page.getByRole('button',{name:'创建待确认任务'}).click()
 await page.getByRole('link',{name:'已创建 查看并确认任务'}).waitFor()
 const batchWrite=writes.findLast(w=>w.path==='/tasks')
 assert.equal(batchWrite.body.title,'批量测试事项')
 assert.ok(batchWrite.headers['idempotency-key'])
 await page.locator('.profile').hover()
 await page.getByText('退出登录',{exact:true}).click()
 await page.getByRole('heading',{name:'登录工作台'}).waitFor()
 assert.equal(await page.locator('.watermark').count(),0,'logout destroys watermark')
 assert.deepEqual(issues,[],'no browser runtime errors')
 await mkdir('tests/results',{recursive:true})
 await writeFile('tests/results/browser-smoke.json',JSON.stringify({mode:'synthetic API fixture only',checks,writes:writes.map(w=>({path:w.path,csrf:!!w.headers['x-csrf-token'],idempotency:!!w.headers['idempotency-key']})),issues},null,2))
 console.log(`PASS fixture smoke: ${checks.length} route/viewport checks, CSRF/idempotency/version action, comment, context switch, admin editor, watermark lifecycle.`)
}catch(e){console.error(JSON.stringify({body:await page.locator("body").innerText(),issues,writes:writes.map(w=>w.path)},null,2));throw e}finally{await browser.close()}
