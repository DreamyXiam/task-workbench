package cn.workbench;
import org.junit.jupiter.api.*;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.test.web.servlet.MockMvc;import org.springframework.mock.web.MockHttpSession;import org.springframework.test.web.servlet.MvcResult;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"task.bootstrap-password=TestOnly-Synthetic12345","task.totp-key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="}) @org.springframework.test.context.TestExecutionListeners(value={org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,org.springframework.test.context.transaction.TransactionalTestExecutionListener.class},mergeMode=org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS) @ActiveProfiles("test") @AutoConfigureMockMvc class HttpSecurityTest {
 @Autowired MockMvc mvc;@Autowired Store db;
 @Test void csrfRejectsForgedLoginBeforeMutation()throws Exception{mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"account\":\"demo.employee\",\"password\":\"TestOnly-Synthetic12345\"}")).andExpect(status().isForbidden());}
 @Test void unauthenticatedCannotReadTasks()throws Exception{mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());}
 @Test void bootstrapHasSeparateScopesAndRealForeignKeys(){assertThat(db.users()).hasSize(7);assertThat(db.list("DEPARTMENT",null,null)).hasSize(3);}
 @Test void employeeCannotGuessForeignTaskOrBypassPasswordChange()throws Exception{var login=mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content("{\"account\":\"demo.employee\",\"password\":\"TestOnly-Synthetic12345\"}")).andExpect(status().isOk()).andReturn();var session=(MockHttpSession)login.getRequest().getSession(false);mvc.perform(get("/api/v1/tasks/99999").session(session)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));}

 @Autowired TaskService tasks;@Autowired Records records;@Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Test @org.springframework.transaction.annotation.Transactional void departmentStatsExcludeDraftAssignmentsAndPreserveOverdueFacts()throws Exception{
  var head=session("demo.head");var actor=new Policy.Actor(2,"COMPANY_LEADER",1,null,2);
  var task=tasks.create(actor,Records.map("title","部门统计草稿","content","测试","companyId",1,"hostDeptId",3,"collaboratorDeptIds",java.util.List.of(),"deadline",java.time.Instant.now().plusSeconds(3600).toString()));
  long taskId=Jsons.number(task.get("id"));
  mvc.perform(get("/api/v1/stats/dept").session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].total").value(0));
  tasks.taskAction(actor,taskId,"confirm-assign",java.util.Map.of());
  var sub=records.lock(Jsons.number(records.children(taskId,"SUB_TASK").getFirst().get("id")));sub.put("deadline",java.time.Instant.now().minusSeconds(60).toString());sub.put("submittedAt",java.time.Instant.now().minusSeconds(120).toString());sub.put("status","ACCEPTED");records.save(sub);
  mvc.perform(get("/api/v1/stats/dept").session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].total").value(1)).andExpect(jsonPath("$.data[0].completed").value(1)).andExpect(jsonPath("$.data[0].overdue").value(0));
  mvc.perform(get("/api/v1/sub-tasks/mine").session(head).param("overdue","true")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
  sub=records.lock(Jsons.number(sub.get("id")));sub.put("everOverdue",true);records.save(sub);
  mvc.perform(get("/api/v1/stats/dept").session(head)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].overdue").value(1));
  mvc.perform(get("/api/v1/sub-tasks/mine").session(head).param("overdue","true")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
 }
 @Test void statsOverdueUsesSubmissionAndSummaryTimes(){
  String due=java.time.Instant.now().minusSeconds(60).toString(),onTime=java.time.Instant.now().minusSeconds(120).toString(),late=java.time.Instant.now().minusSeconds(30).toString();
  assertThat(StatsController.overdue(Records.map("status","SUBMITTED","deadline",due,"submittedAt",onTime,"everOverdue",false))).isFalse();
  assertThat(StatsController.overdue(Records.map("status","SUBMITTED","deadline",due,"submittedAt",late,"everOverdue",false))).isTrue();
  assertThat(StatsController.overdue(Records.map("status","PENDING_ACCEPTANCE","deadline",due,"summarizedAt",onTime,"everOverdue",false))).isFalse();
  assertThat(StatsController.overdue(Records.map("status","IN_PROGRESS","deadline",due,"everOverdue",false))).isTrue();
  assertThat(StatsController.overdue(Records.map("status","ACCEPTED","deadline",due,"everOverdue",false))).isFalse();
  assertThat(StatsController.overdue(Records.map("status","ACCEPTED","deadline",due,"submittedAt",late,"everOverdue",false))).isTrue();
  assertThat(StatsController.overdue(Records.map("status","ACCEPTED","deadline",due,"everOverdue",true))).isTrue();
 }
 @Test @org.springframework.transaction.annotation.Transactional void draftAssignmentIsHiddenFromDepartmentUntilConfirmed()throws Exception{
  var head=session("demo.head");var actor=new Policy.Actor(2,"COMPANY_LEADER",1,null,2);
  var task=tasks.create(actor,Records.map("title","未确认草稿访问测试","content","草稿内容","companyId",1,"hostDeptId",3,"collaboratorDeptIds",java.util.List.of(),"deadline",java.time.Instant.now().plusSeconds(3600).toString()));
  long taskId=Jsons.number(task.get("id"));long subId=Jsons.number(records.children(taskId,"SUB_TASK").getFirst().get("id"));
  mvc.perform(get("/api/v1/tasks").session(head).param("keyword","未确认草稿访问测试")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
  mvc.perform(get("/api/v1/sub-tasks/mine").session(head).param("keyword","未确认草稿访问测试")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
  mvc.perform(get("/api/v1/tasks/"+taskId).session(head)).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/sub-tasks/"+subId+"/receive").session(head).with(csrf()).header("Idempotency-Key",java.util.UUID.randomUUID().toString()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  tasks.taskAction(actor,taskId,"confirm-assign",java.util.Map.of());
  mvc.perform(get("/api/v1/tasks").session(head).param("keyword","未确认草稿访问测试")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
  mvc.perform(get("/api/v1/sub-tasks/mine").session(head).param("keyword","未确认草稿访问测试")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
 }
 @Test @org.springframework.transaction.annotation.Transactional void draftCrossCompanyInvitationCannotBeGuessedOrAccepted()throws Exception{
  var other=session("demo.other");var actor=new Policy.Actor(2,"COMPANY_LEADER",1,null,2);
  var task=tasks.create(actor,Records.map("title","草稿协办","content","测试","companyId",1,"hostDeptId",3,"collaboratorDeptIds",java.util.List.of(5),"deadline",java.time.Instant.now().plusSeconds(3600).toString()));
  var request=records.children(Jsons.number(task.get("id")),"COLLABORATION_REQUEST").getFirst();
  mvc.perform(get("/api/v1/collaborations").session(other)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
  mvc.perform(post("/api/v1/collaborations/"+request.get("id")+"/accept").session(other).with(csrf()).header("Idempotency-Key",java.util.UUID.randomUUID().toString()).contentType("application/json").content("{}")).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/tasks/"+task.get("id")).session(other)).andExpect(status().isForbidden());
 }
 @Test @org.springframework.transaction.annotation.Transactional void archiveOnlyVisibleReadNotifications()throws Exception{
  var employee=session("demo.employee");
  var hidden=tasks.create(new Policy.Actor(2,"COMPANY_LEADER",1,null,2),Records.map("title","不可见任务","content","测试","companyId",1,"hostDeptId",3,"collaboratorDeptIds",java.util.List.of(),"deadline",java.time.Instant.now().plusSeconds(3600).toString()));
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,task_id,title,is_read) VALUES(?,?,?,?,?,?)",20001,4,1,null,"本岗位已读",true);
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,task_id,title,is_read) VALUES(?,?,?,?,?,?)",20002,4,2,null,"其他公司",true);
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,task_id,title,is_read) VALUES(?,?,?,?,?,?)",20003,4,1,hidden.get("id"),"不可见任务通知",true);
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,task_id,title,is_read) VALUES(?,?,?,?,?,?)",20004,4,1,null,"本岗位未读",false);
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,task_id,title,is_read) VALUES(?,?,?,?,?,?)",20005,3,1,null,"其他用户",true);
  mvc.perform(post("/api/v1/notifications/archive").session(employee).with(csrf()).header("Idempotency-Key",java.util.UUID.randomUUID().toString()).contentType("application/json").content("{}")).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("SELECT archived FROM notification WHERE outbox_id=20001",Boolean.class)).isTrue();
  for(long id:new long[]{20002,20003,20004,20005})assertThat(jdbc.queryForObject("SELECT archived FROM notification WHERE outbox_id=?",Boolean.class,id)).as("outbox %s",id).isFalse();
 }
 @Test @org.springframework.transaction.annotation.Transactional void notificationReadFilterRespectsUserCompanyAndPagination()throws Exception{
  var employee=session("demo.employee");
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,title,is_read) VALUES(?,?,?,?,?)",10001,4,1,"未读通知",false);
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,title,is_read) VALUES(?,?,?,?,?)",10002,4,1,"已读通知",true);
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,title,is_read) VALUES(?,?,?,?,?)",10003,4,2,"其他公司通知",false);
  jdbc.update("INSERT INTO notification(outbox_id,user_id,company_id,title,is_read) VALUES(?,?,?,?,?)",10004,3,1,"其他用户通知",false);
  mvc.perform(get("/api/v1/notifications").session(employee).param("read","false").param("pageSize","1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].title").value("未读通知"));
  mvc.perform(get("/api/v1/notifications").session(employee).param("read","true")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].title").value("已读通知"));
  mvc.perform(get("/api/v1/notifications").session(employee)).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2));
 }
 @Test @org.springframework.transaction.annotation.Transactional void taskAndMineFiltersApplyBeforePagination()throws Exception{
  var leaderSession=session("demo.leader");var headSession=session("demo.head");var actor=new Policy.Actor(2,"COMPANY_LEADER",1,null,2);
  var first=tasks.create(actor,Records.map("title","筛选甲","content","测试","companyId",1,"hostDeptId",3,"collaboratorDeptIds",java.util.List.of(),"priority","HIGH","deadline",java.time.Instant.now().plusSeconds(3600).toString()));
  var second=tasks.create(actor,Records.map("title","筛选乙","content","测试","companyId",1,"hostDeptId",3,"collaboratorDeptIds",java.util.List.of(),"priority","LOW","deadline",java.time.Instant.now().plusSeconds(3600).toString()));
  var foreign=tasks.create(new Policy.Actor(7,"GROUP_LEADER",null,null,7),Records.map("title","外公司筛选隔离","content","测试","companyId",2,"hostDeptId",5,"collaboratorDeptIds",java.util.List.of(),"priority","HIGH","deadline",java.time.Instant.now().plusSeconds(3600).toString()));
  tasks.taskAction(actor,Jsons.number(first.get("id")),"confirm-assign",java.util.Map.of());tasks.taskAction(actor,Jsons.number(second.get("id")),"confirm-assign",java.util.Map.of());
  var expired=records.lock(Jsons.number(first.get("id")));expired.put("deadline",java.time.Instant.now().minusSeconds(60).toString());records.save(expired);
  var sub=records.children(Jsons.number(first.get("id")),"SUB_TASK").getFirst();var expiredSub=records.lock(Jsons.number(sub.get("id")));expiredSub.put("deadline",java.time.Instant.now().minusSeconds(60).toString());records.save(expiredSub);
  mvc.perform(get("/api/v1/tasks").session(leaderSession).param("priority","HIGH")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].id").value(first.get("id")));
  mvc.perform(get("/api/v1/tasks").session(leaderSession).param("overdue","true")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
  mvc.perform(get("/api/v1/tasks").session(leaderSession).param("keyword",String.valueOf(second.get("code")))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].id").value(second.get("id")));
  mvc.perform(get("/api/v1/tasks").session(leaderSession).param("keyword","筛选").param("pageSize","1").param("page","2")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2)).andExpect(jsonPath("$.data.items.length()").value(1));
  mvc.perform(get("/api/v1/tasks").session(leaderSession).param("keyword",String.valueOf(foreign.get("code")))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
  mvc.perform(get("/api/v1/sub-tasks/mine").session(headSession).param("priority","HIGH").param("status","PENDING_RECEIVE").param("overdue","true").param("keyword",String.valueOf(first.get("code")))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].code").value(first.get("code")));
  mvc.perform(get("/api/v1/sub-tasks/mine").session(headSession).param("status","ACCEPTED")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
  mvc.perform(get("/api/v1/sub-tasks/mine").session(headSession).param("keyword",String.valueOf(foreign.get("code")))).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
 }
 MockHttpSession session(String account){var user=db.userByAccount(account);db.password(Jsons.number(user.get("id")),passwords.encode("Different-TestPassword123"));user=db.user(Jsons.number(user.get("id")));var session=new MockHttpSession();session.setAttribute("uid",Jsons.number(user.get("id")));session.setAttribute("sessionVersion",Jsons.integer(user.get("sessionVersion")));session.setAttribute("scopeId",Jsons.number(db.scopes(Jsons.number(user.get("id"))).getFirst().get("id")));session.setAttribute("totpVerified",true);return session;}
 @Test @org.springframework.transaction.annotation.Transactional void realIdorAndUnassignedEmployeeReadAreRejected()throws Exception{var employee=session("demo.employee");var foreign=tasks.create(new Policy.Actor(7,"GROUP_LEADER",null,null,7),Records.map("title","乙公司私有任务","content","不可泄漏","companyId",2,"hostDeptId",5,"collaboratorDeptIds",java.util.List.of(),"deadline",java.time.Instant.now().plusSeconds(3600).toString()));mvc.perform(get("/api/v1/tasks/"+foreign.get("id")).session(employee)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));var own=tasks.create(new Policy.Actor(2,"COMPANY_LEADER",1,null,2),Records.map("title","同部门未指派任务","content","不可通过部门猜测访问","companyId",1,"hostDeptId",3,"collaboratorDeptIds",java.util.List.of(),"deadline",java.time.Instant.now().plusSeconds(3600).toString()));mvc.perform(get("/api/v1/tasks/"+own.get("id")).session(employee)).andExpect(status().isForbidden());}
 @Test @org.springframework.transaction.annotation.Transactional void fiveLoginFailuresLockAccountAcrossSessions()throws Exception{var u=Records.map("account","test.locked","name","合成锁定用户","passwordHash",passwords.encode("LockTest-Password123"),"mustChangePassword",false);db.insertUser(u);db.insertScope(Records.map("userId",u.get("id"),"role","EMPLOYEE","companyId",1,"deptId",3));for(int i=0;i<5;i++)mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content("{\"account\":\"test.locked\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content("{\"account\":\"test.locked\",\"password\":\"LockTest-Password123\"}")).andExpect(status().isTooManyRequests());}
 @Test @org.springframework.transaction.annotation.Transactional void disabledUserLosesExistingSession()throws Exception{var s=session("demo.employee");db.enabled(Jsons.number(s.getAttribute("uid")),false);mvc.perform(get("/api/v1/tasks").session(s)).andExpect(status().isUnauthorized());}
 @Test @org.springframework.transaction.annotation.Transactional void attachmentTraversalAndQuarantineAreEnforced()throws Exception{var s=session("demo.head");var task=tasks.create(new Policy.Actor(2,"COMPANY_LEADER",1,null,2),Records.map("title","附件安全测试","content","合成附件","companyId",1,"hostDeptId",3,"collaboratorDeptIds",java.util.List.of(),"deadline",java.time.Instant.now().plusSeconds(3600).toString()));tasks.taskAction(new Policy.Actor(2,"COMPANY_LEADER",1,null,2),Jsons.number(task.get("id")),"confirm-assign",java.util.Map.of());var traversal=new org.springframework.mock.web.MockMultipartFile("file","../payload.txt","text/plain","hello".getBytes());mvc.perform(multipart("/api/v1/attachments").file(traversal).param("taskId",task.get("id").toString()).session(s).with(csrf()).header("Idempotency-Key",java.util.UUID.randomUUID().toString())).andExpect(status().isUnprocessableEntity());var f=new org.springframework.mock.web.MockMultipartFile("file","report.txt","text/plain","hello".getBytes());var result=mvc.perform(multipart("/api/v1/attachments").file(f).param("taskId",task.get("id").toString()).session(s).with(csrf()).header("Idempotency-Key",java.util.UUID.randomUUID().toString())).andExpect(status().isOk()).andExpect(jsonPath("$.data.scanStatus").value("QUARANTINED")).andReturn();var object=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());mvc.perform(get("/api/v1/attachments/"+object.at("/data/id").asLong()+"/download").session(s)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FILE_QUARANTINED"));}

 @Autowired OrganizationController organization;
 @Test void scopedAdminCannotGrantGlobalLeader(){assertThatThrownBy(()->organization.scopes(new Policy.Actor(1,"ADMIN",1,null,1),Records.map("scopes",java.util.List.of(Records.map("role","GROUP_LEADER","companyId",1))))).isInstanceOf(ApiException.class);}
 @Test @org.springframework.transaction.annotation.Transactional void scopedAdminCannotResetSharedAccount()throws Exception{var s=session("demo.admin");s.setAttribute("totpAt",System.currentTimeMillis());var shared=Records.map("account","test.shared","name","跨公司合成用户","passwordHash",passwords.encode("Shared-TestPassword123"),"mustChangePassword",false);db.insertUser(shared);db.insertScope(Records.map("userId",shared.get("id"),"role","EMPLOYEE","companyId",1,"deptId",3));db.insertScope(Records.map("userId",shared.get("id"),"role","EMPLOYEE","companyId",2,"deptId",5));mvc.perform(put("/api/v1/admin/users/"+shared.get("id")).session(s).with(csrf()).header("Idempotency-Key",java.util.UUID.randomUUID().toString()).contentType("application/json").content("{\"password\":\"Takeover-TestPassword123\"}")).andExpect(status().isForbidden());assertThat(passwords.matches("Shared-TestPassword123",String.valueOf(db.user(Jsons.number(shared.get("id"))).get("passwordHash")))).isTrue();}
}
