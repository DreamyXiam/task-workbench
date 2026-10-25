package cn.workbench;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("unchecked")
class MatchingEngineTest {
    final MatchingEngine engine = new MatchingEngine();
    static Map<String,Object> row(Object... fields) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (int i=0;i<fields.length;i+=2) result.put((String)fields[i],fields[i+1]);
        return result;
    }
    static Map<String,Object> dept(long id) { return row("id",id,"companyId",1,"name","Dept "+id,"enabled",true); }
    static Map<String,Object> rule(long id,long dept,String name,Object... fields) {
        Map<String,Object> result=row("id",id,"companyId",1,"deptId",dept,"name",name,"status","ACTIVE","enabled",true,"ruleVersion",1,"weight",100);
        result.putAll(row(fields)); return result;
    }
    Map<String,Object> match(String title,String content,List<Map<String,Object>> tags,List<Map<String,Object>> rules) {
        return engine.match(1,title,content,List.of(dept(3),dept(4),dept(5)),tags,rules);
    }
    @SuppressWarnings("unchecked") static List<Map<String,Object>> suggestions(Map<String,Object> result) { return (List<Map<String,Object>>)result.get("suggestions"); }
    @SuppressWarnings("unchecked") static List<Map<String,Object>> hits(Map<String,Object> suggestion) { return (List<Map<String,Object>>)suggestion.get("hits"); }
    @Test void normalizesWidthCaseAndWhitespaceBeforeMatching() {
        var result=match("  ＴＡＸ\tＲＥＰＯＲＴ ","Budget review",List.of(rule(1,3,"Tax","keywords",List.of("tax report"),"weight",60)),List.of());
        assertThat(suggestions(result)).hasSize(1);
        assertThat(suggestions(result).getFirst()).containsEntry("deptId",3L).containsEntry("score",60);
        assertThat(hits(suggestions(result).getFirst()).getFirst()).containsEntry("term","tax report").containsEntry("weight",60).containsEntry("sourceKind","TAG");
    }
    @Test void longestFixedPhraseBeatsGeneralKeywordRegardlessOfWeight() {
        var result=match("Annual tax report review","",List.of(rule(1,3,"tax","weight",1000)),List.of(rule(2,4,"short","fixedPhrases",List.of("tax report"),"weight",1),rule(3,5,"specific","fixedPhrases",List.of("annual tax report"),"weight",1)));
        assertThat(suggestions(result)).extracting(s->s.get("deptId")).containsExactly(5L,4L,3L);
        assertThat(suggestions(result).getFirst()).containsEntry("role","HOST");
    }
    @Test void exactFieldFixedRuleWinsAndExplainsRuleVersion() {
        var result=match("Tax report","Please prepare the annual tax report",List.of(),List.of(rule(1,3,"longer","fixedPhrases",List.of("annual tax report")),rule(2,4,"exact","fixedPhrases",List.of("tax report"),"ruleVersion",7)));
        assertThat(suggestions(result).getFirst()).containsEntry("deptId",4L);
        assertThat(hits(suggestions(result).getFirst()).getFirst()).containsEntry("matchType","EXACT_FIXED").containsEntry("ruleVersion",7);
    }
    @Test void negatedOccurrencesDoNotScoreAndAffirmativeOccurrencesStillCan() {
        var negative=match("不开票","",List.of(rule(1,3,"开票")),List.of());
        assertThat(suggestions(negative)).isEmpty();
        assertThat(negative).containsEntry("manualFallback",true);
        assertThat((List<?>)negative.get("excludedHits")).isNotEmpty();
        var mixed=match("不开票，随后要求开票","",List.of(rule(1,3,"开票")),List.of());
        assertThat(suggestions(mixed)).hasSize(1);
        assertThat(suggestions(mixed).getFirst()).containsEntry("score",100);
    }
    @Test void explicitNegativeKeywordsSuppressTheRuleAndRemainExplainable() {
        var result=match("tax refund","not sales",List.of(),List.of(rule(1,3,"tax","keywords",List.of("tax"),"negativeKeywords",List.of("refund"))));
        assertThat(suggestions(result)).isEmpty();
        assertThat((List<?>)result.get("excludedHits")).isNotEmpty();
    }
    @Test void companyOverrideSuppressesGlobalRuleIncludingDisabledOverride() {
        var global=rule(1,3,"Tax policy","companyId",0,"keywords",List.of("tax"),"weight",900);
        var local=rule(2,4,"Tax policy","keywords",List.of("tax"),"weight",25);
        var result=match("tax","",List.of(),List.of(global,local));
        assertThat(suggestions(result)).hasSize(1);
        assertThat(suggestions(result).getFirst()).containsEntry("deptId",4L).containsEntry("score",25);
        local.put("enabled",false);
        assertThat(suggestions(match("tax","",List.of(),List.of(global,local)))).isEmpty();
    }
    @Test void foreignCompanyRulesAndForeignDepartmentsNeverBecomeCandidates() {
        var result=engine.match(1,"tax","",List.of(dept(3),row("id",99,"companyId",2,"name","Foreign","enabled",true)),List.of(),List.of(rule(1,3,"foreign rule","companyId",2,"keywords",List.of("tax")),rule(2,99,"global foreigndept","companyId",0,"keywords",List.of("tax"))));
        assertThat(suggestions(result)).isEmpty();
    }
    @Test void deterministicTiesNeedHumanConfirmationAndDoNotDependOnInputOrder() {
        var a=rule(1,4,"a","keywords",List.of("tax")); var b=rule(2,3,"b","keywords",List.of("tax"));
        var first=match("tax","",List.of(a,b),List.of()); var second=match("tax","",List.of(b,a),List.of());
        assertThat(first).isEqualTo(second);
        assertThat(suggestions(first)).extracting(s->s.get("deptId")).containsExactly(3L,4L);
        assertThat(first.get("tiedHostDeptIds")).isEqualTo(List.of(3L,4L));
        assertThat(suggestions(first)).allSatisfy(s->assertThat(s).containsEntry("requiresConfirmation",true));
    }
    @Test void repeatedTermsScoreOnceAndAsciiWordsNeedBoundaries() {
        assertThat(suggestions(match("taxi","",List.of(rule(1,3,"tax")),List.of()))).isEmpty();
        var result=match("tax tax tax","tax",List.of(rule(1,3,"Tax","keywords",List.of("tax","TAX"),"weight",10)),List.of());
        assertThat(suggestions(result).getFirst()).containsEntry("score",10);
        assertThat(hits(suggestions(result).getFirst())).hasSize(1);
    }
    @Test void noMatchKeepsManualFallbackAndNeverInventsADepartment() {
        var result=match("unrelated","",List.of(rule(1,3,"tax")),List.of());
        assertThat(result).containsEntry("manualFallback",true);
        assertThat(suggestions(result)).isEmpty();
        assertThat(result.get("message")).isNotNull();
    }
    @Test void collaboratorOnlyRuleCannotAppointAHost() {
        var result=match("tax","",List.of(),List.of(rule(1,3,"tax","role","COLLABORATOR")));
        assertThat(suggestions(result).getFirst()).containsEntry("role","COLLABORATOR");
        assertThat(result).containsEntry("manualFallback",true);
    }
    @Test void snapshotsDoNotChangeWhenSourceConfigurationChanges() {
        var source=rule(1,3,"tax","keywords",new ArrayList<>(List.of("tax")));
        var result=match("tax","",List.of(),List.of(source));
        String before=result.toString(); source.put("weight",999); ((List<String>)source.get("keywords")).clear();
        assertThat(result.toString()).isEqualTo(before);
        assertThatThrownBy(()->suggestions(result).clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void sameTermCanSuggestMultipleDepartmentsWithinTheSameScope() {
        var result=match("tax","",List.of(rule(1,3,"Tax"),rule(2,4,"Tax")),List.of());
        assertThat(suggestions(result)).extracting(s->s.get("deptId")).containsExactly(3L,4L);
    }
    @Test void englishNegationRequiresAWordBoundary() {
        assertThat(suggestions(match("knot tax","",List.of(rule(1,3,"Tax")),List.of()))).hasSize(1);
        assertThat(suggestions(match("not tax","",List.of(rule(1,3,"Tax")),List.of()))).isEmpty();
    }

    @Test void fixedCollaboratorRoleCannotBeOverriddenByAnOrdinaryHostTag() {
        var result=match("tax report","",List.of(rule(1,3,"tax")),List.of(rule(2,3,"fixed collaborator","fixedPhrases",List.of("tax report"),"role","COLLABORATOR")));
        assertThat(suggestions(result).getFirst()).containsEntry("role","COLLABORATOR");
        assertThat(result).containsEntry("manualFallback",true);
    }

    @org.junit.jupiter.api.Nested
    @org.springframework.boot.test.context.SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:matching;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","task.bootstrap-password=TestOnly-Synthetic12345","task.totp-key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="})
    @org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
    @org.springframework.test.context.ActiveProfiles("test")
    @org.springframework.test.context.TestExecutionListeners(value={org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,org.springframework.test.context.transaction.TransactionalTestExecutionListener.class},mergeMode=org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
    class FeedbackApi {
        @org.springframework.beans.factory.annotation.Autowired OrganizationController organization;
        @org.springframework.beans.factory.annotation.Autowired org.springframework.test.web.servlet.MockMvc http;
        @org.springframework.beans.factory.annotation.Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
        @org.springframework.beans.factory.annotation.Autowired MatchingController feedback;
        @org.springframework.beans.factory.annotation.Autowired Records records;
        @org.springframework.beans.factory.annotation.Autowired Store db;
        @org.springframework.beans.factory.annotation.Autowired TaskService tasks;
        @org.springframework.beans.factory.annotation.Autowired org.springframework.transaction.PlatformTransactionManager transactions;
        final Policy.Actor leader=new Policy.Actor(2,"COMPANY_LEADER",1,null,2);
        org.springframework.mock.web.MockHttpServletRequest request(String account,String path){
            var user=db.userByAccount(account);long id=Jsons.number(user.get("id"));
            var request=new org.springframework.mock.web.MockHttpServletRequest("POST",path);
            request.getSession().setAttribute("uid",id);request.getSession().setAttribute("scopeId",db.scopes(id).getFirst().get("id"));
            request.addHeader("Idempotency-Key",UUID.randomUUID().toString());return request;
        }
        @SuppressWarnings("unchecked") Map<String,Object> data(Object envelope){return (Map<String,Object>)((Map<String,Object>)envelope).get("data");}
        Map<String,Object> matchResult(){
            records.create("RULE",1,3,null,2L,"ACTIVE",Records.map("name","Report policy","ruleVersion",1,"keywords",List.of("report"),"weight",25));
            return data(organization.match(Records.map("companyId",1,"title","Report","content","Synthetic requirement"),request("demo.leader","/api/v1/tasks/match")));
        }
        Map<String,Object> task(int host){return new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->tasks.create(leader,Records.map("companyId",1,"title","Report","content","Synthetic requirement","hostDeptId",host,"collaboratorDeptIds",List.of(),"deadline",java.time.Instant.now().plusSeconds(3600).toString())));}
        org.springframework.mock.web.MockHttpSession httpSession(String account){
            var user=db.userByAccount(account);long id=Jsons.number(user.get("id"));
            db.password(id,passwords.encode("Synthetic-NewPassword123"));
            var session=(org.springframework.mock.web.MockHttpSession)request(account,"/api/v1/organization/collaboration-departments").getSession();
            session.setAttribute("sessionVersion",Jsons.integer(db.user(id).get("sessionVersion")));session.setAttribute("totpVerified",true);return session;
        }
        @Test @org.springframework.transaction.annotation.Transactional
        void collaborationOptionsAreLeaderOnlyAndExposeOnlyFourApprovedFields()throws Exception{
            http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/organization/collaboration-departments").session(httpSession("demo.employee")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
            var session=httpSession("demo.leader");
            var response=http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/organization/collaboration-departments").session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
            var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getResponse().getContentAsString()).get("data");
            assertThat(json.size()).isEqualTo(3);
            for(var department:json){List<String> fields=new ArrayList<>();department.fieldNames().forEachRemaining(fields::add);assertThat(fields).containsExactlyInAnyOrder("id","name","companyId","companyName");}
            assertThat(json.findValuesAsText("companyId")).contains("2");
            var disabled=records.get(2);disabled.put("enabled",false);records.save(disabled);
            http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/organization/collaboration-departments").session(session))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.length()").value(2));
        }
        @Test @org.springframework.transaction.annotation.Transactional
        void onlyExplicitGlobalAdministratorCanCreateGlobalRules(){
            var input=Records.map("scope","GLOBAL","name","Global policy","deptId",3,"keywords",List.of("report"),"weight",90);
            var scoped=request("demo.admin","/api/v1/admin/rules");scoped.getSession().setAttribute("totpAt",System.currentTimeMillis());
            assertThatThrownBy(()->organization.create("rules",input,scoped)).isInstanceOf(ApiException.class);
            var global=Records.map("userId",1,"role","ADMIN","companyId",null,"deptId",null);db.insertScope(global);
            var elevated=request("demo.admin","/api/v1/admin/rules");elevated.getSession().setAttribute("scopeId",global.get("id"));elevated.getSession().setAttribute("totpAt",System.currentTimeMillis());
            organization.create("rules",input,elevated);
            assertThat(records.list("RULE",0,null)).hasSize(1);
            var leaderRequest=request("demo.leader","/api/v1/tasks/match");
            var result=data(organization.match(Records.map("companyId",1,"title","Report","content","Synthetic requirement"),leaderRequest));
            assertThat(suggestions(result).getFirst()).containsEntry("deptId",3L).containsEntry("score",90);
        }
        @Test @org.springframework.transaction.annotation.Transactional
        void feedbackUsesStoredSuggestionAndActualAssignmentWithoutPublishingRules(){
            var result=matchResult();var task=task(4);
            var response=data(feedback.submit(Records.map("matchingResultId",result.get("matchingResultId"),"taskId",task.get("id"),"decision","ACCEPTED","reason","Human override"),request("demo.leader","/api/v1/matching/feedback")));
            assertThat(response).containsEntry("decision","OVERRIDDEN").containsEntry("actualHostDeptId",4);
            assertThat(records.children(Jsons.number(task.get("id")),"MATCHING_FEEDBACK")).hasSize(1);
            assertThat(records.list("RULE",1,null)).allSatisfy(rule->assertThat(rule).containsEntry("ruleVersion",1));
            var replay=data(feedback.submit(Records.map("matchingResultId",result.get("matchingResultId"),"taskId",task.get("id"),"reason","Repeated"),request("demo.leader","/api/v1/matching/feedback")));
            assertThat(replay.get("id")).isEqualTo(response.get("id"));
            assertThat(records.children(Jsons.number(task.get("id")),"MATCHING_FEEDBACK")).hasSize(1);
        }
        @Test @org.springframework.transaction.annotation.Transactional
        void feedbackRejectsForeignScopeAndMismatchedInput(){
            var result=matchResult();var task=task(3);
            assertThatThrownBy(()->feedback.submit(Records.map("matchingResultId",result.get("matchingResultId"),"taskId",task.get("id"),"reason","Guess"),request("demo.other","/api/v1/matching/feedback"))).isInstanceOf(ApiException.class);
            var stored=records.get(Jsons.number(result.get("matchingResultId")));stored.put("inputHash","different");records.save(stored);
            assertThatThrownBy(()->feedback.submit(Records.map("matchingResultId",result.get("matchingResultId"),"taskId",task.get("id"),"reason","Different input"),request("demo.leader","/api/v1/matching/feedback"))).isInstanceOf(ApiException.class);
        }
        @Test @org.springframework.transaction.annotation.Transactional
        void statisticsEnforceCompanyAndTimeWindow(){
            var result=matchResult();var task=task(3);
            feedback.submit(Records.map("matchingResultId",result.get("matchingResultId"),"taskId",task.get("id"),"reason","Accepted"),request("demo.leader","/api/v1/matching/feedback"));
            var request=request("demo.leader","/api/v1/matching/feedback");
            var future=data(feedback.list(1,1,20,java.time.Instant.now().plusSeconds(3600).toString(),null,request));
            assertThat((Map<String,Object>)future.get("summary")).containsEntry("sampleCount",0).containsEntry("adoptionRate",null);
            assertThatThrownBy(()->feedback.list(2,1,20,null,null,request)).isInstanceOf(ApiException.class);
            assertThatThrownBy(()->feedback.list(1,1,20,"invalid",null,request)).isInstanceOf(ApiException.class);
        }
        @Test @org.springframework.transaction.annotation.Transactional
        void suggestionSnapshotSurvivesRuleChangesAndAdoptionIsNotAccuracy(){
            var result=matchResult();var task=task(3);var rule=records.list("RULE",1,null).getFirst();rule.put("weight",999);records.save(rule);
            var historical=data(feedback.result(Jsons.number(result.get("matchingResultId")),request("demo.leader","/api/v1/matching/results")));
            assertThat(suggestions(historical).getFirst()).containsEntry("score",25);
            feedback.submit(Records.map("matchingResultId",result.get("matchingResultId"),"taskId",task.get("id"),"reason","Accepted"),request("demo.leader","/api/v1/matching/feedback"));
            var listing=data(feedback.list(1,1,20,null,null,request("demo.leader","/api/v1/matching/feedback")));
            assertThat((Map<String,Object>)listing.get("summary")).containsKeys("adoptionRate","sampleCount");
            assertThat((Map<String,Object>)listing.get("summary")).doesNotContainKey("accuracy");
        }
    }
}
