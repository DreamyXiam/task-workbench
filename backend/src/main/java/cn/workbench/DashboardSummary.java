package cn.workbench;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import static cn.workbench.Records.map;

@Service public class DashboardSummary {
 final Records records;final Policy policy;final WorkItemService workItems;
 public DashboardSummary(Records records,Policy policy,WorkItemService workItems){this.records=records;this.policy=policy;this.workItems=workItems;}
 static boolean active(Map<String,Object> task){return !Set.of("COMPLETED","CANCELLED").contains(task.get("status"));}
 Map<String,Object> summarize(Policy.Actor actor,Map<String,Object> task,Instant now){policy.task(actor,TaskService.number(task,"id"));return enrich(actor,task,signals(actor,task,now));}
 @SuppressWarnings("unchecked") Map<String,Object> signals(Policy.Actor actor,Map<String,Object> task,Instant now){
  long id=TaskService.number(task,"id");int company=TaskService.integer(task,"companyId"),host=TaskService.integer(task,"hostDeptId");
  boolean home=actor.globalAdmin()||actor.leader()&&Domain.companyAllowed(actor.role(),actor.company(),company)||actor.departmentHead()&&Objects.equals(actor.company(),company)&&Objects.equals(actor.dept(),host);
  var row=workItems.db.latestVisibleProgress(id,home,actor.company(),actor.leader(),actor.dept());var progress=row==null?null:records.view(row,true);
  String note=progress==null?null:String.valueOf(progress.get("note")),source=progress==null?null:"部门进度";Instant updated=progress==null?null:Records.instant(progress.get("createdAt"));Object percent=progress==null?null:progress.get("percent"),progressDept=progress==null?null:progress.get("deptId");
  var key=KeyWorkService.data(task);var fields=KeyWorkService.fields(key.get("fields"));var preset=fields.stream().filter(f->"progress".equals(f.get("id"))&&"推进情况".equals(f.get("name"))&&"TEXT".equals(f.get("type"))).findFirst().orElse(null);Object keyProgress=KeyWorkService.values(key).get("progress");
  if(preset!=null&&keyProgress instanceof String text&&!text.isBlank())for(var history:records.children(id,"KEY_WORK_HISTORY")){if(!"FILL".equals(history.get("status")))continue;Object old=history.get("before"),next=history.get("after");if(!(old instanceof Map<?,?>)||!(next instanceof Map<?,?>))continue;var before=KeyWorkService.values((Map<String,Object>)old);var after=KeyWorkService.values((Map<String,Object>)next);if(!Objects.equals(before.get("progress"),after.get("progress"))&&Objects.equals(after.get("progress"),text)){Instant at=Records.instant(history.get("createdAt"));if(updated==null||at.isAfter(updated)){note=text;updated=at;source="重点工作填报";percent=null;progressDept=null;}}}
  boolean running=active(task)&&!Set.of("PENDING_ACCEPTANCE","PENDING_ACCEPTANCE_WITH_GAP").contains(task.get("status"));List<String> flags=new ArrayList<>();int rank=active(task)?4:5;Instant deadline=Records.instant(task.get("deadline"));if(running&&deadline.isBefore(now)&&task.get("summarizedAt")==null){flags.add("已逾期");rank=0;}
  if(active(task)&&Boolean.TRUE.equals(key.get("enabled"))&&Set.of("PENDING","IN_PROGRESS").contains(key.get("coordinationStatus"))){flags.add("待领导协调");rank=Math.min(rank,1);}
  if(running&&!deadline.isBefore(now)&&deadline.isBefore(now.plus(Duration.ofDays(3)))){flags.add("3天内到期");rank=Math.min(rank,2);}
  Instant since=updated==null?Records.instant(task.get("createdAt")):updated;if(running&&!"DRAFT".equals(task.get("status"))&&since.isBefore(now.minus(Duration.ofDays(7)))){flags.add("7天未见进展更新");rank=Math.min(rank,3);}
  return map("latestProgress",note,"progressSource",source,"progressAt",updated==null?null:updated.toString(),"reportedPercent",percent,"progressDeptId",progressDept,"attentionFlags",flags,"attentionRank",rank);
 }
 Map<String,Object> enrich(Policy.Actor actor,Map<String,Object> task,Map<String,Object> summary){long id=TaskService.number(task,"id");int company=TaskService.integer(task,"companyId"),host=TaskService.integer(task,"hostDeptId");boolean home=actor.globalAdmin()||actor.leader()&&Domain.companyAllowed(actor.role(),actor.company(),company)||actor.departmentHead()&&Objects.equals(actor.company(),company)&&Objects.equals(actor.dept(),host);var subs=records.children(id,"SUB_TASK").stream().filter(s->{if(home)return true;try{policy.subtask(actor,s,false);return true;}catch(ApiException denied){return false;}}).toList();var hostSub=subs.stream().filter(s->Boolean.TRUE.equals(s.get("isHost"))).findFirst().orElse(null);String owner=null;if(hostSub!=null&&hostSub.get("ownerId")!=null){var user=workItems.db.user(Jsons.number(hostSub.get("ownerId")));if(user!=null)owner=String.valueOf(user.get("name"));}var items=workItems.forTask(actor,task);summary.put("hostOwnerName",owner);summary.put("hostVisible",hostSub!=null);summary.put("workItemTotal",items.size());summary.put("workItemCompleted",items.stream().filter(i->"COMPLETED".equals(i.get("status"))).count());Object dept=summary.remove("progressDeptId");summary.put("progressDeptName",dept==null?null:records.get(Jsons.number(dept)).get("name"));return summary;}
}
