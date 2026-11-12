package cn.workbench;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController @RequestMapping("/api/v1/audit") public class AuditController {
 final AuditLogService audit;final Policy policy;final StatsController exports;final Writes writes;
 public AuditController(AuditLogService audit,Policy policy,StatsController exports,Writes writes){this.audit=audit;this.policy=policy;this.exports=exports;this.writes=writes;}
 @GetMapping Object list(HttpServletRequest q,@RequestParam(required=false)Integer companyId,@RequestParam(required=false)String keyword,@RequestParam(required=false)String action,@RequestParam(required=false)String from,@RequestParam(required=false)String to,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return Api.ok(audit.page(policy.actor(q),companyId,keyword,action,from,to,page,pageSize));}
 @GetMapping("/export") ResponseEntity<byte[]> export(HttpServletRequest q,@RequestParam(required=false)Integer companyId,@RequestParam(required=false)String keyword,@RequestParam(required=false)String action,@RequestParam(required=false)String from,@RequestParam(required=false)String to,@RequestParam(defaultValue="xlsx")String format){var a=policy.actor(q);audit.filter(a,companyId,keyword,action,from,to);policy.freshTotp(q);return exports.bounded(a,()->{var result=exports.document(a,"audit-log",format,List.of("日志编号","操作时间（UTC）","操作人","操作人ID","公司","公司ID","操作说明","动作编码","对象类型","对象编号","代理授权ID","追踪号"),audit.export(a,companyId,keyword,action,from,to));writes.audit(a,"AUDIT_EXPORT_"+format.toUpperCase(Locale.ROOT),0);return result;});}
}
