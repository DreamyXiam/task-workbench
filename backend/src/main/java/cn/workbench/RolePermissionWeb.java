package cn.workbench;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration public class RolePermissionWeb implements WebMvcConfigurer {
 final Policy policy;final RolePermissions permissions;
 public RolePermissionWeb(Policy policy,RolePermissions permissions){this.policy=policy;this.permissions=permissions;}
 @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){@Override public boolean preHandle(HttpServletRequest q,HttpServletResponse response,Object handler){String required=RolePermissions.required(q);if(required!=null){var actor=policy.actor(q);var denied=actor.globalAdmin()?java.util.Set.<String>of():permissions.disabled(actor.role());boolean userDirectory=required.equals("ORG_ADMIN")&&q.getMethod().equals("GET")&&q.getRequestURI().equals("/api/v1/admin/companies")&&actor.administrator()&&!denied.contains("USER_ADMIN");if("UNMAPPED".equals(required)||denied.contains(required)&&!userDirectory||denied.contains("TASK_VIEW")&&RolePermissions.requiresTaskView(required)||actor.administrator()&&q.getRequestURI().startsWith("/api/v1/organization/appointments")&&denied.contains("USER_ADMIN"))throw new ApiException(403,"ROLE_PERMISSION_DENIED","当前角色未获此功能权限");}return true;}}).addPathPatterns("/api/v1/**");}
}
