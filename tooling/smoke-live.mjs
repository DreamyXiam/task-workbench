/**
 * Destructive ONLY to synthetic demo accounts/tasks on a loopback test instance.
 * TASK_SMOKE_BOOTSTRAP_PASSWORD stays in process memory; never logged.
 * Run: node tooling/smoke-live.mjs --ack-synthetic
 */
import assert from 'node:assert/strict';
import { randomBytes, randomUUID, createHmac } from 'node:crypto';
import { writeFile } from 'node:fs/promises';
const base = new URL(process.env.TASK_SMOKE_BASE || 'http://127.0.0.1:18150');
assert(['127.0.0.1','localhost'].includes(base.hostname) && process.argv.includes('--ack-synthetic'), 'Explicit loopback synthetic test acknowledgement required');
const bootstrap = process.env.TASK_SMOKE_BOOTSTRAP_PASSWORD;
assert(bootstrap && bootstrap.length >= 12, 'Synthetic bootstrap credential required');
const checks = [];
function check(name, fn) { return Promise.resolve().then(fn).then(() => checks.push({name,result:'PASS'})); }
function otp(secret) {
  const alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'; let bits='', bytes=[];
  for(const c of secret.toUpperCase().replace(/=+$/,'')) bits += alphabet.indexOf(c).toString(2).padStart(5,'0');
  for(let i=0;i+8<=bits.length;i+=8) bytes.push(parseInt(bits.slice(i,i+8),2));
  const time=Buffer.alloc(8);time.writeBigUInt64BE(BigInt(Math.floor(Date.now()/30000)));
  const h=createHmac('sha1',Buffer.from(bytes)).update(time).digest(),o=h.at(-1)&15;
  return ((h.readUInt32BE(o)&0x7fffffff)%1000000).toString().padStart(6,'0');
}
class Session {
  cookies = new Map(); csrf = null;
  constructor(account) { this.account=account; this.password=bootstrap; }
  async call(path,{method='GET',body,key=randomUUID(),csrf=true}={}) {
    const headers={cookie:[...this.cookies].map(([a,b])=>a+'='+b).join(';')};
    if(body!==undefined) headers['content-type']='application/json';
    if(method!=='GET' && !path.startsWith('/auth/')) headers['Idempotency-Key']=key;
    if(csrf && this.csrf) headers[this.csrf.headerName]=this.csrf.token;
    const r=await fetch(new URL('/api/v1'+path,base),{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
    for(const c of r.headers.getSetCookie()){const [k,v]=c.split(';')[0].split('=');this.cookies.set(k,v);}
    const text=await r.text();let json;try{json=JSON.parse(text);}catch{json={message:'Non-JSON response'};}
    return {status:r.status,json,data:json.data};
  }
  async token(){const r=await this.call('/auth/csrf');assert.equal(r.status,200,'CSRF bootstrap');this.csrf=r.data;}
  async login(){await this.token();const r=await this.call('/auth/login',{method:'POST',body:{account:this.account,password:this.password}});assert.equal(r.status,200,'synthetic login '+this.account);await this.token();return r.data;}
  async prepare(){
    let me=await this.login();
    if(me.mustChangePassword){const next='Synthetic4x'+randomBytes(18).toString('hex');const c=await this.call('/auth/change-password',{method:'POST',body:{oldPassword:this.password,newPassword:next}});assert.equal(c.status,200,'initial password change');this.password=next;me=await this.login();}
    if(me.totpRequired){const s=await this.call('/auth/totp/setup',{method:'POST'});assert.equal(s.status,200,'TOTP enroll');const c=await this.call('/auth/totp/confirm',{method:'POST',body:{code:otp(s.data.secret)}});assert.equal(c.status,200,'TOTP confirm');assert(c.data.recoveryCodes.length>=1,'recovery codes generated');}
    return this;
  }
}
const anon=new Session('anonymous');
try {
  await check('Anonymous protected endpoint returns 401',async()=>assert.equal((await anon.call('/tasks')).status,401));
  await check('Missing CSRF login rejected',async()=>{await anon.token();assert.equal((await anon.call('/auth/login',{method:'POST',body:{account:'demo.employee',password:bootstrap},csrf:false})).status,403);});
  const leader=await new Session('demo.leader').prepare(),head=await new Session('demo.head').prepare(),finance=await new Session('demo.finance').prepare(),other=await new Session('demo.other').prepare();
  checks.push({name:'Initial password rotation and mandatory leader TOTP',result:'PASS'});
  const payload={title:'Synthetic integration '+randomUUID(),content:'Synthetic workflow fixtures only',companyId:1,hostDeptId:3,collaboratorDeptIds:[4],priority:'HIGH',deadline:new Date(Date.now()+7*86400000).toISOString()};
  const key=randomUUID();const created=await leader.call('/tasks',{method:'POST',body:payload,key});
  assert.equal(created.status,200,'task create: '+created.json.code);let task=created.data;
  await check('Company daily task number',()=>assert.match(task.code,/^RW-[A-Z0-9]+-\d{8}-\d+$/));
  await check('Idempotent task creation',async()=>{const r=await leader.call('/tasks',{method:'POST',body:payload,key});assert.equal(r.status,200);assert.equal(r.data.id,task.id);});
  await check('Key reuse with different payload rejected',async()=>assert.equal((await leader.call('/tasks',{method:'POST',body:{...payload,title:payload.title+' changed'},key})).status,409));
  await check('Cross-company guessed task ID rejected',async()=>assert([403,404].includes((await other.call('/tasks/'+task.id)).status)));
  await check('Concurrent same-key writes produce one task',async()=>{const parallelKey=randomUUID(),body={...payload,title:'Concurrent synthetic '+randomUUID()};const responses=await Promise.all(Array.from({length:8},()=>leader.call('/tasks',{method:'POST',body,key:parallelKey})));assert(responses.every(r=>r.status===200),'all concurrent replays return200');assert.equal(new Set(responses.map(r=>r.data.id)).size,1);});
  let result=await leader.call('/tasks/'+task.id+'/confirm-assign',{method:'POST',body:{version:task.version}});assert.equal(result.status,200,'confirm assignment');
  let detail=(await head.call('/tasks/'+task.id)).data;
  const host=detail.subTasks.find(s=>s.isHost),collab=detail.subTasks.find(s=>!s.isHost);
  assert(host && collab,'host and collaborator created');
  let h=await head.call('/sub-tasks/'+host.id+'/receive',{method:'POST',body:{}});assert.equal(h.status,200,'host receive');
  let f=await finance.call('/sub-tasks/'+collab.id+'/receive',{method:'POST',body:{}});assert.equal(f.status,200,'collaborator receive');
  h=await head.call('/sub-tasks/'+host.id+'/submit',{method:'POST',body:{summary:'Host synthetic result',version:h.data.version}});assert.equal(h.status,200,'host submit');
  f=await finance.call('/sub-tasks/'+collab.id+'/submit',{method:'POST',body:{summary:'Collaborator synthetic result',version:f.data.version}});assert.equal(f.status,200,'collaborator submit');
  detail=(await head.call('/tasks/'+task.id)).data;
  let summary=await head.call('/tasks/'+task.id+'/summarize',{method:'POST',body:{version:detail.version,allowGap:false}});assert.equal(summary.status,200,'host summarize');
  await check('Withdrawal allowed before final acceptance and invalidates summary',async()=>{
    const d=(await head.call('/tasks/'+task.id)).data,s=d.subTasks.find(v=>v.id===host.id);
    const w=await head.call('/sub-tasks/'+host.id+'/withdraw',{method:'POST',body:{version:s.version,reason:'Synthetic correction'}});
    assert.equal(w.status,200,'pre-acceptance withdrawal');
    const now=(await head.call('/tasks/'+task.id)).data;assert(['IN_PROGRESS','REWORK'].includes(now.status));
    const sv=now.subTasks.find(v=>v.id===host.id);
    const submit=await head.call('/sub-tasks/'+host.id+'/submit',{method:'POST',body:{version:sv.version,summary:'Corrected host result'}});assert.equal(submit.status,200);
    const again=(await head.call('/tasks/'+task.id)).data;summary=await head.call('/tasks/'+task.id+'/summarize',{method:'POST',body:{version:again.version,allowGap:false}});assert.equal(summary.status,200);
  });
  let current=(await leader.call('/tasks/'+task.id)).data;
  const reject=await leader.call('/tasks/'+task.id+'/reject',{method:'POST',body:{version:current.version,deptIds:[4],reason:'Synthetic rework'}});assert.equal(reject.status,200);
  await check('Rework returns canonical REWORK state',()=>assert.equal(reject.data.status,'REWORK'));
  for(const [session,subId] of [[head,host.id],[finance,collab.id]]){const d=(await session.call('/tasks/'+task.id)).data;const s=d.subTasks.find(v=>v.id===subId);const r=await session.call('/sub-tasks/'+subId+'/submit',{method:'POST',body:{version:s.version,summary:'Reworked synthetic result'}});assert.equal(r.status,200);}
  await check('Cannot accept rework without fresh host summary',async()=>{current=(await leader.call('/tasks/'+task.id)).data;assert.equal((await leader.call('/tasks/'+task.id+'/accept',{method:'POST',body:{version:current.version}})).status,409);});
  current=(await head.call('/tasks/'+task.id)).data;const sum2=await head.call('/tasks/'+task.id+'/summarize',{method:'POST',body:{version:current.version,allowGap:false}});assert.equal(sum2.status,200);
  current=(await leader.call('/tasks/'+task.id)).data;
  await check('Concurrent final acceptance has one winner and explicit409 loser',async()=>{const responses=await Promise.all([1,2].map(()=>leader.call('/tasks/'+task.id+'/accept',{method:'POST',body:{version:current.version}})));assert.deepEqual(responses.map(r=>r.status).sort(),[200,409]);});
  await check('Completed task cannot be withdrawn',async()=>{const d=(await head.call('/tasks/'+task.id)).data;const s=d.subTasks.find(v=>v.id===host.id);assert.equal((await head.call('/sub-tasks/'+host.id+'/withdraw',{method:'POST',body:{version:s.version,reason:'Not allowed'}})).status,409);});
  checks.push({name:'Create → receive → submit → summarize → rework → re-summarize → accept',result:'PASS'});
  await check('KPI export requires fresh TOTP and employee cannot export',async()=>{assert.equal((await head.call('/kpi/export?format=xlsx')).status,403);});
  await check('Logout revokes prior session',async()=>{assert.equal((await leader.call('/auth/logout',{method:'POST'})).status,200);assert.equal((await leader.call('/tasks')).status,401);});
  console.log(JSON.stringify({result:'PASS',checks},null,2));
} catch(e) {
  checks.push({name:e.message,result:'FAIL'});
  console.error(JSON.stringify({result:'FAIL',checks},null,2));process.exitCode=1;
} finally {
  if(process.env.TASK_SMOKE_REPORT)await writeFile(process.env.TASK_SMOKE_REPORT,JSON.stringify({checks},null,2)+'\n');
}
