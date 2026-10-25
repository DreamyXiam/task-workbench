package cn.workbench;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static cn.workbench.Records.map;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:review;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","task.bootstrap-password=TestOnly-Synthetic12345","task.totp-key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="})
@org.springframework.test.context.TestExecutionListeners(value={org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,org.springframework.test.context.transaction.TransactionalTestExecutionListener.class},mergeMode=org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@ActiveProfiles("test") @Transactional class ReviewRegressionTest {
 @Autowired TaskService tasks; @Autowired Records records; @Autowired Store db; @Autowired Policy policy;
 @Autowired KpiController kpi; @Autowired AttachmentController attachments; @Autowired Writes writes;
 @Autowired Jobs jobs; @Autowired Jsons json; @Autowired PlatformTransactionManager manager; @Autowired JdbcTemplate jdbc;
 final Policy.Actor leader=new Policy.Actor(2,"COMPANY_LEADER",1,null,2), head=new Policy.Actor(3,"DEPT_HEAD",1,3,3), employee=new Policy.Actor(4,"EMPLOYEE",1,3,4), admin=new Policy.Actor(1,"ADMIN",1,null,1);
 long id(Map<String,Object>x){return Jsons.number(x.get("id"));}
 Map<String,Object> create(){return tasks.create(leader,map("title","Review synthetic task","content","Synthetic only","companyId",1,"hostDeptId",3,"collaboratorDeptIds",List.of(4),"deadline",Instant.now().plusSeconds(3600).toString()));}
 MockHttpServletRequest request(long user,long scope){var q=new MockHttpServletRequest();q.getSession().setAttribute("uid",user);q.getSession().setAttribute("scopeId",scope);return q;}
 @Test void malformedGlobalRoleDoesNotGrantAnyCompany(){assertThat(Domain.companyAllowed("GROUP_LEADER",1,2)).isFalse();}
 @Test void departmentRunResponseNeverContainsForeignTaskSnapshot(){
  var task=create();var sub=records.children(id(task),"SUB_TASK").stream().filter(s->Jsons.integer(s.get("deptId"))==4).findFirst().orElseThrow();
  sub.put("deadline","2020-04-20T00:00:00Z");sub.put("summary","PRIVATE_OTHER_DEPARTMENT_RESULT");records.save(sub);
  var template=records.list("KPI_TEMPLATE",null,null).getFirst();
  kpi.run(admin,map("companyId",1,"period","2020-04","templateId",template.get("id"),"simulation",true));
  assertThat(json.write(kpi.runs(request(3,3),1))).doesNotContain("PRIVATE_OTHER_DEPARTMENT_RESULT").doesNotContain("inputSnapshot");
 }
 @Test void taskLevelAttachmentStillRequiresDepartmentPermission(){
  var task=create();tasks.taskAction(leader,id(task),"confirm-assign",Map.of());
  var host=records.children(id(task),"SUB_TASK").stream().filter(s->Jsons.integer(s.get("deptId"))==3).findFirst().orElseThrow();
  tasks.subAction(head,id(host),"receive",Map.of());tasks.subAction(head,id(host),"assign",map("userIds",List.of(4),"ownerId",4));
  var file=records.create("ATTACHMENT",1,4,id(task),5L,"CLEAN",map("subTaskId",null,"uploaderId",5,"storageKey","synthetic.bin","originalName","result.txt","mimeType","text/plain","sizeBytes",5,"sha256","0".repeat(64),"scanStatus","CLEAN"));
  assertThatThrownBy(()->attachments.authorized(employee,id(file))).isInstanceOf(ApiException.class);
 }
 @Test void kpiDoesNotChargeOtherDepartmentForParentLateness(){
  var s=map("status","ACCEPTED","deadline","2020-01-10T00:00:00Z","submittedAt","2020-01-09T00:00:00Z","firstReceivedAt","2020-01-08T00:00:00Z","dispatchedAt","2020-01-08T00:00:00Z","rejectCount",0,"everOverdue",false,"parentEverOverdue",true);
  assertThat(KpiController.decimal(KpiController.metrics(List.of(s)).get("overdueRate"))).isEqualByComparingTo("0");
 }
 @Test void reworkRateUsesAcceptedTasksAsItsDenominator(){
  var a=map("status","ACCEPTED","deadline","2020-01-10T00:00:00Z","submittedAt","2020-01-09T00:00:00Z","rejectCount",1);
  var b=map("status","IN_PROGRESS","deadline","2020-01-10T00:00:00Z","rejectCount",0);
  assertThat(KpiController.decimal(KpiController.metrics(List.of(a,b)).get("reworkRate"))).isEqualByComparingTo("100");
 }
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) void concurrentAuditReadsKeepOneChain() throws Exception {
  try(var pool=Executors.newFixedThreadPool(6)){var start=new CountDownLatch(1);var work=new ArrayList<Future<?>>();
   for(int i=0;i<30;i++){final int n=i;work.add(pool.submit(()->{try{start.await();writes.audit(head,"SYNTHETIC_PARALLEL_READ",n);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}}));}start.countDown();for(var f:work)f.get(20,TimeUnit.SECONDS);
  }
  var rows=jdbc.queryForList("SELECT previous_hash,event_hash FROM audit_event ORDER BY id");String previous="GENESIS";
  for(var row:rows){assertThat(row.get("previous_hash")).isEqualTo(previous);previous=String.valueOf(row.get("event_hash"));}
  assertThat(jdbc.queryForObject("SELECT last_hash FROM audit_chain_anchor WHERE id=1",String.class)).isEqualTo(previous);
 }
 @Test void regularSchedulerCatchesMissedMonthlySettlementOnce(){
  var scheduler=new Jobs(db,records,kpi,manager,true);scheduler.reminders();var template=records.list("KPI_TEMPLATE",null,null).getFirst();var key=map("company",1,"period",YearMonth.now(ZoneId.of("Asia/Shanghai")).minusMonths(1).toString(),"template",template.get("id"));
  assertThat(db.runFor(key)).isNotNull();long first=db.runFor(key);scheduler.reminders();assertThat(db.runFor(key)).isEqualTo(first);
 }
 @Test void unassignedEmployeeNeverReceivesTaskMetadata(){
  var task=create();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE task_id=? AND recipient_id=4",Long.class,id(task))).isZero();
  tasks.taskAction(leader,id(task),"confirm-assign",Map.of());
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE task_id=? AND recipient_id=4",Long.class,id(task))).isZero();
 }

 @Test void globalTagsUseNullableRelationalScope(){var tag=records.create("TAG",0,3,null,1L,"ACTIVE",map("name","全局标签","weight",100));assertThat(tag.get("companyId")).isNull();assertThat(records.list("TAG",0,null)).anyMatch(t->id(t)==id(tag));}
}