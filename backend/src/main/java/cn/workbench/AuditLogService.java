package cn.workbench;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import static cn.workbench.Records.map;

/** Explicit safe projection: audit records never serialize user credentials or business payloads. */
@Service public class AuditLogService {
 final JdbcTemplate jdbc;
 public AuditLogService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 static final Map<String,String> ACTIONS=Map.ofEntries(
  Map.entry("USER_CREATE","创建用户与岗位"),Map.entry("USER_ENABLED","调整用户启用状态"),Map.entry("COMPANY_CREATE","创建公司"),
  Map.entry("APPOINTMENT_CHANGE","调整岗位任职"),Map.entry("DELEGATION_CREATE","创建限时代理"),Map.entry("DELEGATION_REVOKE","撤销代理"),Map.entry("DELEGATION_ACTIVATE","进入代理上下文"),Map.entry("RESPONSIBILITY_HANDOVER","交接部门任务"),Map.entry("ROLE_PERMISSION_UPDATE","修改角色权限"),
  Map.entry("FORM_COLLECTION_CREATE","发起部门填表"),Map.entry("FORM_RESPONSE_DRAFT","保存填表草稿"),Map.entry("FORM_RESPONSE_SUBMIT","提交部门填表"),Map.entry("FORM_COLLECTION_EXPORT","导出部门填表汇总"),
  Map.entry("KPI_SIMULATE","模拟计算考核"),Map.entry("KPI_PUBLISH","发布考核结果"),Map.entry("KPI_APPEAL","提出考核申诉"),Map.entry("KPI_APPEAL_RESOLVE","处理考核申诉"),Map.entry("KPI_ADJUST","调整考核分数"),Map.entry("KPI_UNLOCK","解锁考核批次"),Map.entry("KPI_CALENDAR_SIMULATE","模拟日历考核"),Map.entry("KPI_CANDIDATE_CREATE","登记考核候选模板"),Map.entry("CALENDAR_CANDIDATE_CREATE","登记日历候选模板"),Map.entry("TASK_CLASSIFICATION_APPEND","追加任务分类"),
  Map.entry("MATCHING_SUGGEST","生成职能匹配建议"),Map.entry("MATCHING_FEEDBACK_ACCEPTED","采纳匹配建议"),Map.entry("MATCHING_FEEDBACK_OVERRIDDEN","调整匹配建议"),
  Map.entry("CREATE","创建任务"),Map.entry("CONFIRM_ASSIGN","确认任务分配"),Map.entry("RECEIVE","接收任务"),Map.entry("ASSIGN","分派人员"),Map.entry("PROGRESS","更新任务进展"),Map.entry("SUBMIT","提交成果"),Map.entry("WITHDRAW","撤回成果"),Map.entry("SUMMARIZE","汇总上报"),Map.entry("ACCEPT","验收任务"),Map.entry("REJECT","退回修改"),Map.entry("CANCEL","取消任务"),Map.entry("DEADLINE","调整截止日期"),Map.entry("URGE","催办任务"),Map.entry("COMMENT","发表意见"),Map.entry("EXTEND","申请延期"),Map.entry("TRANSFER","转交任务"));
 static String label(String action){
  if(ACTIONS.containsKey(action))return ACTIONS.get(action);
  String normalized=action.replace("-","_");if(normalized.startsWith("SUBTASK_"))normalized=normalized.substring(8);if(ACTIONS.containsKey(normalized))return ACTIONS.get(normalized);
  if(action.equals("DEADLINE_CHANGED"))return "调整截止日期";
  if(action.equals("EXTENSION_REQUEST"))return "申请延期";if(Set.of("EXTENSION_APPROVE","EXTENSION_APPROVED").contains(action))return "批准延期";if(Set.of("EXTENSION_REJECT","EXTENSION_REJECTED").contains(action))return "拒绝延期";
  if(action.equals("COLLABORATION_ACCEPT"))return "接受跨公司协同";if(action.equals("COLLABORATION_REJECT"))return "拒绝跨公司协同";
  if(action.equals("WORK_ITEM_CREATE"))return "创建子任务";if(action.equals("WORK_ITEM_UPDATE"))return "更新子任务";
  if(action.equals("ATTACHMENT_DOWNLOAD"))return "下载附件";if(action.equals("ATTACHMENT_PREVIEW"))return "预览附件";
  if(action.startsWith("ATTACHMENT_UPLOAD_"))return "上传附件 · "+Map.of("CLEAN","扫描通过","PENDING","待扫描","INFECTED","已隔离","UNAVAILABLE","扫描不可用").getOrDefault(action.substring(18),action.substring(18));
  for(var prefix:Map.of("TASK_EXPORT_","导出任务统计","KPI_EXPORT_","导出考核结果","AUDIT_EXPORT_","导出操作日志").entrySet())if(action.startsWith(prefix.getKey()))return prefix.getValue()+" · "+action.substring(prefix.getKey().length());
  for(var prefix:Map.of("ADMIN_CREATE_","创建","ADMIN_UPDATE_","更新").entrySet())if(action.startsWith(prefix.getKey()))return prefix.getValue()+Map.of("companies","公司","departments","部门","tags","职能标签","rules","匹配规则","notification-channels","通知渠道").getOrDefault(action.substring(prefix.getKey().length()),"管理配置");
  if(action.startsWith("ATTACHMENT_"))return "附件操作 · "+action.substring(11);
  if(action.startsWith("WORK_ITEM_"))return "子任务操作 · "+action.substring(10);
  return action;
 }
 static String objectType(String action){
  if(action.startsWith("USER_"))return "用户";
  if(action.startsWith("APPOINTMENT_"))return "任职岗位";
  if(action.startsWith("DELEGATION_"))return "代理授权";
  if(action.startsWith("ROLE_PERMISSION_"))return "权限变更";
  if(action.startsWith("ADMIN_")||action.equals("COMPANY_CREATE"))return "组织配置";
  if(action.startsWith("AUDIT_"))return "操作日志";
  if(action.startsWith("KPI_"))return "部门考核";
  if(action.startsWith("FORM_"))return "部门填表";
  if(action.startsWith("ATTACHMENT_"))return "附件";
  if(action.startsWith("MATCHING_"))return "职能匹配";
  return "任务与执行";
 }
 record Filter(List<Object> args){}
 static final String WHERE=" WHERE (? IS NULL OR e.company_id=?) AND (? IS NULL OR e.trace_id LIKE ? ESCAPE '!' OR e.action LIKE ? ESCAPE '!' OR u.name LIKE ? ESCAPE '!' OR c.name LIKE ? ESCAPE '!') AND (? IS NULL OR e.action=?) AND (? IS NULL OR e.created_at>=?) AND (? IS NULL OR e.created_at<?)";
 Filter filter(Policy.Actor actor,Integer company,String keyword,String action,String from,String to){
  if(!actor.leader()&&!actor.administrator())throw ApiException.denied();
  boolean global=actor.globalAdmin()||actor.role().equals("GROUP_LEADER")&&actor.company()==null;
  if(!global&&(actor.company()==null||company!=null&&!Objects.equals(actor.company(),company)))throw ApiException.denied();
  var args=new ArrayList<Object>();Integer scope=global?company:actor.company();args.add(scope);args.add(scope);
  String like=null;if(keyword!=null&&!keyword.isBlank()){if(keyword.length()>100)throw TaskService.invalid("关键词最多100字");like="%"+keyword.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";}for(int i=0;i<5;i++)args.add(like);
  String code=action==null||action.isBlank()?null:action;if(code!=null&&code.length()>100)throw TaskService.invalid("动作筛选无效");args.add(code);args.add(code);
  LocalDate start=null,end=null;try{if(from!=null&&!from.isBlank())start=LocalDate.parse(from);if(to!=null&&!to.isBlank())end=LocalDate.parse(to);}catch(Exception e){throw TaskService.invalid("日期格式应为YYYY-MM-DD");}
  if(start!=null&&end!=null&&start.isAfter(end))throw TaskService.invalid("开始日期不能晚于结束日期");
  Timestamp since=start==null?null:Timestamp.from(start.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());Timestamp until=end==null?null:Timestamp.from(end.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());args.add(since);args.add(since);args.add(until);args.add(until);
  return new Filter(args);
 }
 static final String FROM=" FROM audit_event e LEFT JOIN app_user u ON u.id=e.actor_id LEFT JOIN company c ON c.id=e.company_id";
 List<Map<String,Object>> rows(Filter f,int limit,int offset){var args=new ArrayList<>(f.args());args.add(limit);args.add(offset);return jdbc.query("SELECT e.id,e.actor_id,u.name,e.company_id,c.name,e.action,e.object_id,e.trace_id,e.delegation_id,e.created_at"+FROM+WHERE+" ORDER BY e.id DESC LIMIT ? OFFSET ?",(rs,n)->map("id",rs.getLong(1),"actorId",rs.getLong(2),"actorName",Objects.requireNonNullElse(rs.getString(3),"已移除人员"),"companyId",rs.getInt(4),"companyName",rs.getInt(4)==0?"全局":Objects.requireNonNullElse(rs.getString(5),"历史公司"),"action",rs.getString(6),"actionLabel",label(rs.getString(6)),"objectType",objectType(rs.getString(6)),"objectId",rs.getLong(7),"traceId",rs.getString(8),"delegationId",rs.getObject(9),"createdAt",rs.getTimestamp(10).toInstant().toString()),args.toArray());}
 public Map<String,Object> page(Policy.Actor a,Integer company,String keyword,String action,String from,String to,int page,int size){
  if(page<1||page>100000||size<1||size>100)throw TaskService.invalid("分页参数无效");var f=filter(a,company,keyword,action,from,to);long total=jdbc.queryForObject("SELECT COUNT(*)"+FROM+WHERE,Long.class,f.args().toArray());
  var scope=filter(a,company,null,null,null,null);var actions=jdbc.query("SELECT DISTINCT e.action"+FROM+WHERE+" ORDER BY e.action LIMIT 200",(rs,n)->map("value",rs.getString(1),"label",label(rs.getString(1))),scope.args().toArray());
  return map("items",rows(f,size,(page-1)*size),"total",total,"page",page,"pageSize",size,"actions",actions);
 }
 public List<List<String>> export(Policy.Actor a,Integer company,String keyword,String action,String from,String to){return rows(filter(a,company,keyword,action,from,to),ExportFormats.MAX_ROWS+1,0).stream().map(r->List.of(StatsController.value(r,"id"),StatsController.value(r,"createdAt"),StatsController.value(r,"actorName"),StatsController.value(r,"actorId"),StatsController.value(r,"companyName"),StatsController.value(r,"companyId"),StatsController.value(r,"actionLabel"),StatsController.value(r,"action"),StatsController.value(r,"objectType"),StatsController.value(r,"objectId"),StatsController.value(r,"delegationId"),StatsController.value(r,"traceId"))).toList();}
}
