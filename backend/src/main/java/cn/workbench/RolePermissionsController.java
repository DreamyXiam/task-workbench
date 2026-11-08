package cn.workbench;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/role-permissions") public class RolePermissionsController {
 final Policy policy;final RolePermissions permissions;final Writes writes;
 public RolePermissionsController(Policy policy,RolePermissions permissions,Writes writes){this.policy=policy;this.permissions=permissions;this.writes=writes;}
 @GetMapping Object list(HttpServletRequest q){var a=policy.actor(q);if(!a.globalAdmin())throw ApiException.denied();return Api.ok(Records.map("catalog",RolePermissions.CATALOG,"roles",permissions.list()));}
 @PutMapping("/{role}") Object save(@PathVariable String role,@RequestBody Map<String,Object> body,HttpServletRequest q){var a=policy.actor(q);if(!a.globalAdmin())throw ApiException.denied();policy.freshTotp(q);return writes.run(q,body,()->permissions.save(a,role,body));}
}
