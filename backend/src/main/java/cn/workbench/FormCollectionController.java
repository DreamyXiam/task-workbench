package cn.workbench;
import org.springframework.web.bind.annotation.*;import org.springframework.http.ResponseEntity;import jakarta.servlet.http.HttpServletRequest;import java.util.*;
@RestController @RequestMapping("/api/v1/collections") public class FormCollectionController {
 final FormCollectionService forms;final Policy p;final Writes w;final StatsController exports;
 public FormCollectionController(FormCollectionService f,Policy p,Writes w,StatsController e){forms=f;this.p=p;this.w=w;exports=e;}
 @GetMapping Object list(HttpServletRequest q){return Api.ok(forms.list(p.actor(q)));}
 @PostMapping Object create(HttpServletRequest q,@RequestBody Map<String,Object> b){var a=p.actor(q);return w.run(q,b,()->forms.create(a,b));}
 @GetMapping("/{id}") Object detail(HttpServletRequest q,@PathVariable long id){return Api.ok(forms.detail(p.actor(q),id));}
 @PostMapping("/{id}/{action:draft|submit}") Object save(HttpServletRequest q,@PathVariable long id,@PathVariable String action,@RequestBody Map<String,Object>b){var a=p.actor(q);return w.run(q,b,()->forms.save(a,id,action.equals("submit"),b));}
 @GetMapping("/{id}/export") ResponseEntity<byte[]> export(HttpServletRequest q,@PathVariable long id){var a=p.actor(q);p.freshTotp(q);return exports.bounded(a,()->{var rows=forms.exportRows(a,id);var headers=new ArrayList<>(List.of("部门","序号"));headers.addAll(FormCollectionService.LABELS);var result=exports.document(a,"department-collection","xlsx",headers,rows);w.audit(a,"FORM_COLLECTION_EXPORT",id);return result;});}
}
