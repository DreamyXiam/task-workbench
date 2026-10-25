package cn.workbench;

import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/** Durable delivery worker. The external HTTP effect cannot be atomically committed with MySQL. */
public final class WecomDelivery {
 interface Sender { void send(URI endpoint,String json) throws Exception; }
 private final Store db;
 private final Records records;
 private final TransactionTemplate tx;
 private final Sender sender;
 private final Function<String,String> secrets;
 private final Clock clock;
 private static final int MAX_ATTEMPTS=8;

 public WecomDelivery(Store db,Records records,TransactionTemplate tx){
  this(db,records,tx,new HttpSender(records.json),System::getenv,Clock.systemUTC());
 }
 WecomDelivery(Store db,Records records,TransactionTemplate tx,Sender sender,Function<String,String> secrets,Clock clock){
  this.db=db;this.records=records;this.tx=tx;this.sender=sender;this.secrets=secrets;this.clock=clock;
 }
 private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),ZoneId.systemDefault()).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);}

 public void run(){
  // Never expose an uncommitted business transaction to an external boundary.
  if(TransactionSynchronizationManager.isActualTransactionActive())return;
  tx.execute(status->{
   for(var row:db.pendingOutbox()){
    long id=Jsons.number(row.get("id"));
    db.stageDelivery(id,"STATION",0,now());
    for(var channel:records.list("CHANNEL",Jsons.integer(row.get("companyId")),null))
     if(active(channel))db.stageDelivery(id,"WECOM",Jsons.number(channel.get("id")),now());
    db.dispatchOutbox(id);
   }
   return null;
  });
  for(long id:db.dueDeliveries(now())){
   var row=claim(id);
   if(row==null)continue;
   try{
    if("STATION".equals(row.get("channelType")))station(row);
    else external(row);
   }catch(Exception failure){
    if(failure instanceof InterruptedException)Thread.currentThread().interrupt();
    String code=failure instanceof DeliveryFailure known?known.code:"DELIVERY_FAILURE";
    tx.execute(status->{
     db.failDelivery(id,String.valueOf(row.get("claimToken")),Jsons.integer(row.get("attempts"))>=MAX_ATTEMPTS?"DEAD":"PENDING",now().plusSeconds(Math.min(3600,1L<<Math.min(12,Jsons.integer(row.get("attempts"))))),code);
     db.aggregateDelivery(Jsons.number(row.get("outboxId")));return null;
    });
   }
  }
 }
 private Map<String,Object> claim(long id){
  return tx.execute(status->{
   var row=db.lockDelivery(id);
   if(row==null||!due(row))return null;
   if(Jsons.integer(row.get("attempts"))>=MAX_ATTEMPTS&&"IN_FLIGHT".equals(row.get("status"))){
    db.failDelivery(id,String.valueOf(row.get("leaseToken")),"DEAD",now(),"ATTEMPTS_EXHAUSTED");
    db.aggregateDelivery(Jsons.number(row.get("outboxId")));return null;
   }
   String token=UUID.randomUUID().toString();
   db.claimDelivery(id,now().plusSeconds(60),token);
   row.put("claimToken",token);row.put("attempts",Jsons.integer(row.get("attempts"))+1);
   return row;
  });
 }
 private void station(Map<String,Object> row){
  tx.execute(status->{
   var current=db.lockDelivery(Jsons.number(row.get("id")));
   if(!Objects.equals(current.get("leaseToken"),row.get("claimToken"))||!"IN_FLIGHT".equals(current.get("status")))return null;
   long outbox=Jsons.number(row.get("outboxId"));
   db.deliver(outbox);finish(row,"SENT",null);return null;
  });
 }
 private void external(Map<String,Object> row) throws Exception {
  var channel=records.get(Jsons.number(row.get("channelId")));
  if(!"CHANNEL".equals(channel.get("kind"))||Jsons.integer(channel.get("companyId"))!=Jsons.integer(row.get("companyId")))throw new DeliveryFailure("CHANNEL_SCOPE");
  if(!active(channel)){tx.execute(status->{finish(row,"SKIPPED","CHANNEL_DISABLED");return null;});return;}
  int company=Jsons.integer(row.get("companyId"));
  String name="TASK_WECOM_COMPANY_"+company+"_KEY";
  if(!name.equals(channel.get("envKey")))throw new DeliveryFailure("CHANNEL_CONFIGURATION");
  String key=secrets.apply(name);
  if(key==null||!key.matches("[A-Za-z0-9_-]{8,200}"))throw new DeliveryFailure("KEY_UNAVAILABLE");
  URI endpoint=URI.create("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key="+key);
  String content="Task Workbench event "+row.get("outboxId")+"\n"+row.get("title")+(row.get("taskId")==null?"":"\nTask reference: "+row.get("taskId"));
  String body=records.json.write(Map.of("msgtype","text","text",Map.of("content",content)));
  // Claim is committed; no database transaction or row lock spans this network request.
  sender.send(endpoint,body);
  tx.execute(status->{finish(row,"SENT",null);return null;});
 }
 private void finish(Map<String,Object> row,String status,String error){
  db.completeDelivery(Jsons.number(row.get("id")),String.valueOf(row.get("claimToken")),status,error);
  db.aggregateDelivery(Jsons.number(row.get("outboxId")));
 }
 private static boolean active(Map<String,Object> channel){return Boolean.TRUE.equals(channel.get("enabled"))&&"ACTIVE".equals(channel.get("status"))&&"WECOM".equals(channel.get("type"));}
 private boolean due(Map<String,Object> row){return "PENDING".equals(row.get("status"))&&!time(row.get("nextAttempt")).isAfter(now())||"IN_FLIGHT".equals(row.get("status"))&&!time(row.get("leaseUntil")).isAfter(now());}
 private static LocalDateTime time(Object value){return value instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)value;}
 static final class DeliveryFailure extends Exception {
  final String code;DeliveryFailure(String code){super(code);this.code=code;}
 }
 static final class HttpSender implements Sender {
  private static final class ClientHolder { static final HttpClient SHARED=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build(); }
  private final HttpClient client;private final Jsons json;
  HttpSender(Jsons json){this(null,json);}
  HttpSender(HttpClient client,Jsons json){this.client=client;this.json=json;}
  public void send(URI endpoint,String body) throws Exception {
   if(!"https".equals(endpoint.getScheme())||!"qyapi.weixin.qq.com".equals(endpoint.getHost())||!"/cgi-bin/webhook/send".equals(endpoint.getRawPath())||(endpoint.getPort()!=-1&&endpoint.getPort()!=443)||endpoint.getUserInfo()!=null||endpoint.getFragment()!=null||endpoint.getRawQuery()==null||!endpoint.getRawQuery().matches("key=[A-Za-z0-9_-]{8,200}"))throw new DeliveryFailure("ENDPOINT_REJECTED");
   var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8)).header("Content-Type","application/json; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build();
   var future=(client==null?ClientHolder.SHARED:client).sendAsync(request,info->new LimitedBody());
   try{
    var response=future.get(8,java.util.concurrent.TimeUnit.SECONDS);
    validateReply(response.statusCode(),response.body(),json);
   }catch(java.util.concurrent.TimeoutException|InterruptedException interrupted){future.cancel(true);throw interrupted;}
  }
  static void validateReply(int status,byte[] bytes,Jsons json) throws DeliveryFailure {
   if(status!=200)throw new DeliveryFailure("WECOM_HTTP_FAILURE");
   if(bytes.length>65536)throw new DeliveryFailure("WECOM_RESPONSE_INVALID");
   try{
    Object code=json.map(new String(bytes,StandardCharsets.UTF_8)).get("errcode");
    if(!(code instanceof Number)||new java.math.BigDecimal(code.toString()).compareTo(java.math.BigDecimal.ZERO)!=0)throw new DeliveryFailure("WECOM_REJECTED");
   }catch(DeliveryFailure known){throw known;}catch(Exception malformed){throw new DeliveryFailure("WECOM_RESPONSE_INVALID");}
  }
  /** Completion follows the complete bounded body, so the HTTP request deadline covers its read. */
  static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
   private final java.util.concurrent.CompletableFuture<byte[]> result=new java.util.concurrent.CompletableFuture<>();
   private final java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
   private java.util.concurrent.Flow.Subscription subscription;
   public java.util.concurrent.CompletionStage<byte[]> getBody(){return result;}
   public void onSubscribe(java.util.concurrent.Flow.Subscription subscription){this.subscription=subscription;subscription.request(1);}
   public void onNext(List<java.nio.ByteBuffer> chunks){
    if(result.isDone())return;
    for(var chunk:chunks){
     if(bytes.size()+chunk.remaining()>65536){subscription.cancel();result.completeExceptionally(new DeliveryFailure("WECOM_RESPONSE_INVALID"));return;}
     byte[] part=new byte[chunk.remaining()];chunk.get(part);bytes.writeBytes(part);
    }
    subscription.request(1);
   }
   public void onError(Throwable error){result.completeExceptionally(error);}
   public void onComplete(){result.complete(bytes.toByteArray());}
  }
 }
}
