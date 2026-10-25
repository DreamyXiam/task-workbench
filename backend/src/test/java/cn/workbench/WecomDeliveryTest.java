package cn.workbench;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static cn.workbench.Records.map;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:wecom;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","task.bootstrap-password=TestOnly-Synthetic12345","task.totp-key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="})
@org.springframework.test.context.TestExecutionListeners(value={org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class},mergeMode=org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@ActiveProfiles("test") class WecomDeliveryTest {
 @Autowired Store db; @Autowired Records records; @Autowired KpiController kpi;
 @Autowired PlatformTransactionManager manager; @Autowired JdbcTemplate jdbc;
 @Test void stationDeliveryHasDurableStatusAndRestartIsIdempotent(){
  String key="SYNTHETIC-"+UUID.randomUUID();
  db.outbox(map("key",key,"company",1,"recipient",4,"task",null,"title","Synthetic station event"));
  new Jobs(db,records,kpi,manager,true).outbox();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name)='notification_delivery'",Integer.class)).as("independent durable per-channel delivery table").isEqualTo(1);
  long outbox=jdbc.queryForObject("SELECT id FROM notification_outbox WHERE event_key=?",Long.class,key);
  assertThat(jdbc.queryForList("SELECT status FROM notification_delivery WHERE outbox_id=? AND channel_type='STATION'",outbox)).hasSize(1).allSatisfy(row->assertThat(row.get("status")).isEqualTo("SENT"));
  new Jobs(db,records,kpi,manager,true).outbox();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE outbox_id=?",Integer.class,outbox)).isEqualTo(1);
 }

 @BeforeEach void resetDeliveryFixtures(){
  jdbc.update("DELETE FROM notification_delivery");jdbc.update("DELETE FROM notification");jdbc.update("DELETE FROM notification_outbox");
  new TransactionTemplate(manager).execute(status->{for(var c:records.list("CHANNEL",null,null)){c.put("enabled",false);records.save(c);}return null;});
 }
 long event(int company){String key="SYNTHETIC-"+UUID.randomUUID();db.outbox(map("key",key,"company",company,"recipient",4,"task",null,"title","Synthetic scoped event"));return jdbc.queryForObject("SELECT id FROM notification_outbox WHERE event_key=?",Long.class,key);}
 long channel(int company,String envKey){return new TransactionTemplate(manager).execute(status->Jsons.number(records.create("CHANNEL",company,null,null,1L,"ACTIVE",map("name","Synthetic WeCom","type","WECOM","enabled",true,"envKey",envKey)).get("id")));}
 WecomDelivery worker(WecomDelivery.Sender sender,java.util.function.Function<String,String> secrets,Clock clock){return new WecomDelivery(db,records,new TransactionTemplate(manager),sender,secrets,clock);}
 Map<String,Object> external(long outbox){return jdbc.queryForMap("SELECT * FROM notification_delivery WHERE outbox_id=? AND channel_type='WECOM'",outbox);}
 @Test void enabledCompanyChannelSendsOnlyAfterCommitAndDoesNotRepeat(){
  channel(1,"TASK_WECOM_COMPANY_1_KEY");channel(2,"TASK_WECOM_COMPANY_2_KEY");long outbox=event(1);
  var bodies=new ArrayList<String>();var lookups=new ArrayList<String>();
  var worker=worker((uri,body)->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();assertThat(uri.getScheme()).isEqualTo("https");assertThat(uri.getHost()).isEqualTo("qyapi.weixin.qq.com");assertThat(uri.getPath()).isEqualTo("/cgi-bin/webhook/send");bodies.add(body);},name->{lookups.add(name);return "synthetic_key_company_1";},Clock.systemUTC());
  worker.run();worker.run();
  assertThat(bodies).hasSize(1).allSatisfy(body->assertThat(body).doesNotContain("synthetic_key_company_1").contains("Synthetic scoped event"));
  assertThat(lookups).containsExactly("TASK_WECOM_COMPANY_1_KEY");
  assertThat(external(outbox).get("status")).isEqualTo("SENT");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE outbox_id=?",Integer.class,outbox)).isEqualTo(1);
 }
 @Test void retryAndRestartPreserveIndependentStationDeliveryAndSanitizeErrors(){
  channel(1,"TASK_WECOM_COMPANY_1_KEY");long outbox=event(1);var clock=new MutableClock();var calls=new AtomicInteger();
  WecomDelivery.Sender sender=(uri,body)->{if(calls.incrementAndGet()==1)throw new IllegalStateException("DO_NOT_LEAK_synthetic_key_company_1");};
  worker(sender,name->"synthetic_key_company_1",clock).run();
  var first=external(outbox);assertThat(first.get("status")).isEqualTo("PENDING");assertThat(((Number)first.get("attempts")).intValue()).isEqualTo(1);assertThat(String.valueOf(first.get("last_error"))).doesNotContain("DO_NOT_LEAK","synthetic_key");
  worker(sender,name->"synthetic_key_company_1",clock).run();assertThat(calls.get()).isEqualTo(1);
  clock.advance(3);worker(sender,name->"synthetic_key_company_1",clock).run();
  assertThat(calls.get()).isEqualTo(2);assertThat(external(outbox).get("status")).isEqualTo("SENT");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE outbox_id=?",Integer.class,outbox)).isEqualTo(1);
 }
 @Test void expiredClaimRestartsAndOldClaimCannotAcknowledgeNewDelivery(){
  channel(1,"TASK_WECOM_COMPANY_1_KEY");long outbox=event(1);var clock=new MutableClock();
  worker((uri,body)->{throw new IllegalStateException("synthetic transport failure");},name->"synthetic_key_company_1",clock).run();
  long id=((Number)external(outbox).get("id")).longValue();
  jdbc.update("UPDATE notification_delivery SET status='IN_FLIGHT',lease_token='old-claim',lease_until=? WHERE id=?",java.sql.Timestamp.from(clock.instant().minusSeconds(1)),id);
  var calls=new AtomicInteger();worker((uri,body)->calls.incrementAndGet(),name->"synthetic_key_company_1",clock).run();
  assertThat(calls.get()).isEqualTo(1);assertThat(db.completeDelivery(id,"old-claim","SENT",null)).isZero();assertThat(external(outbox).get("status")).isEqualTo("SENT");
 }
 @Test void activeOrRolledBackBusinessTransactionCannotDeliverExternally(){
  channel(1,"TASK_WECOM_COMPANY_1_KEY");var calls=new AtomicInteger();var worker=worker((uri,body)->calls.incrementAndGet(),name->"synthetic_key_company_1",Clock.systemUTC());
  new TransactionTemplate(manager).execute(status->{event(1);worker.run();assertThat(calls.get()).isZero();status.setRollbackOnly();return null;});
  worker.run();assertThat(calls.get()).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_delivery",Integer.class)).isZero();
 }
 @Test void disabledChannelNeverLooksUpKeyOrCallsTransport(){
  long channel=channel(1,"TASK_WECOM_COMPANY_1_KEY");new TransactionTemplate(manager).execute(status->{var c=records.lock(channel);c.put("enabled",false);records.save(c);return null;});event(1);
  worker((uri,body)->{throw new AssertionError("disabled channel transport");},name->{throw new AssertionError("disabled channel secret lookup");},Clock.systemUTC()).run();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_delivery WHERE channel_type='WECOM'",Integer.class)).isZero();
 }
 @Test void foreignSecretReferenceCannotResolveAnotherCompanyKey(){
  channel(1,"TASK_WECOM_COMPANY_2_KEY");long outbox=event(1);
  worker((uri,body)->{throw new AssertionError("invalid channel transport");},name->{throw new AssertionError("foreign secret lookup");},Clock.systemUTC()).run();
  assertThat(external(outbox).get("status")).isEqualTo("PENDING");assertThat(((Number)external(outbox).get("attempts")).intValue()).as("persisted external attempt: %s",external(outbox)).isEqualTo(1);
 }

 @Test void repeatedFailuresBecomeTerminalWithoutRepeatingStation(){
  channel(1,"TASK_WECOM_COMPANY_1_KEY");long outbox=event(1);var clock=new MutableClock();var calls=new AtomicInteger();
  var worker=worker((uri,body)->{calls.incrementAndGet();throw new IllegalStateException("synthetic unavailable");},name->"synthetic_key_company_1",clock);
  for(int i=0;i<10;i++){worker.run();clock.advance(3601);}
  assertThat(calls.get()).isEqualTo(8);assertThat(external(outbox).get("status")).isEqualTo("DEAD");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE outbox_id=?",Integer.class,outbox)).isEqualTo(1);
 }
 @Test void disablingPendingChannelSkipsRetryWithoutAnotherExternalEffect(){
  long channel=channel(1,"TASK_WECOM_COMPANY_1_KEY");long outbox=event(1);var clock=new MutableClock();var calls=new AtomicInteger();
  var worker=worker((uri,body)->{calls.incrementAndGet();throw new IllegalStateException("synthetic unavailable");},name->"synthetic_key_company_1",clock);worker.run();
  new TransactionTemplate(manager).execute(status->{var c=records.lock(channel);c.put("enabled",false);records.save(c);return null;});clock.advance(3);worker.run();
  assertThat(calls.get()).isEqualTo(1);assertThat(external(outbox).get("status")).isEqualTo("SKIPPED");
 }
 @Test void untrustedWebhookEndpointsAreRejectedBeforeAnyHttpClientIsCreated(){
  var transport=new WecomDelivery.HttpSender(records.json);
  for(String uri:List.of("http://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=synthetic_key","https://evil.invalid/cgi-bin/webhook/send?key=synthetic_key","https://qyapi.weixin.qq.com:444/cgi-bin/webhook/send?key=synthetic_key","https://qyapi.weixin.qq.com/cgi-bin/webhook/other?key=synthetic_key","https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=synthetic_key&redirect=x","https://user@qyapi.weixin.qq.com/cgi-bin/webhook/send?key=synthetic_key","https://qyapi.weixin.qq.com/cgi-bin/webhook/%73end?key=synthetic_key","https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=synthetic_key#fragment"))
   assertThatThrownBy(()->transport.send(java.net.URI.create(uri),"{}")).isInstanceOf(WecomDelivery.DeliveryFailure.class).hasMessage("ENDPOINT_REJECTED");
 }
 @Test void responseRequiresHttpSuccessAndExplicitZeroVendorCode(){
  assertThatCode(()->WecomDelivery.HttpSender.validateReply(200,"{\"errcode\":0}".getBytes(java.nio.charset.StandardCharsets.UTF_8),records.json)).doesNotThrowAnyException();
  for(String body:List.of("{}","{\"errcode\":1}","{\"errcode\":0.5}","{\"errcode\":\"0\"}","not json"))
   assertThatThrownBy(()->WecomDelivery.HttpSender.validateReply(200,body.getBytes(java.nio.charset.StandardCharsets.UTF_8),records.json)).isInstanceOf(WecomDelivery.DeliveryFailure.class);
  assertThatThrownBy(()->WecomDelivery.HttpSender.validateReply(302,"{\"errcode\":0}".getBytes(java.nio.charset.StandardCharsets.UTF_8),records.json)).isInstanceOf(WecomDelivery.DeliveryFailure.class);
 }
 @Test void excessiveResponseCancelsSubscriptionWithoutBufferingIt(){
  var body=new WecomDelivery.HttpSender.LimitedBody();var cancelled=new java.util.concurrent.atomic.AtomicBoolean();
  body.onSubscribe(new java.util.concurrent.Flow.Subscription(){public void request(long count){}public void cancel(){cancelled.set(true);}});
  body.onNext(List.of(java.nio.ByteBuffer.wrap(new byte[65537])));
  assertThat(cancelled.get()).isTrue();assertThat(body.getBody().toCompletableFuture()).isCompletedExceptionally();
 }
 static final class MutableClock extends Clock {
  Instant instant=Instant.now();void advance(long seconds){instant=instant.plusSeconds(seconds);}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return instant;}
 }
}
