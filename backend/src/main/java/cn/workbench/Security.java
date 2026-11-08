package cn.workbench;
import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.web.*;import org.springframework.security.web.csrf.*;import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.web.filter.OncePerRequestFilter;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.context.SecurityContextHolder;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.*;import java.util.*;
@Configuration public class Security {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityFilterChain chain(HttpSecurity http,Store store,Jsons json,TestAccessPolicy testAccess)throws Exception{
 var csrf=new HttpSessionCsrfTokenRepository();csrf.setHeaderName("X-CSRF-TOKEN");
 http.csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
 http.authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/**","/actuator/health","/error").permitAll().anyRequest().authenticated());
 http.exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->write(r,401,"UNAUTHENTICATED","请先登录",json)).accessDeniedHandler((q,r,x)->write(r,403,"FORBIDDEN","无权操作或CSRF验证失败",json)));
 http.requestCache(c->c.disable()).formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable());
 http.headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; frame-ancestors 'none'")).referrerPolicy(c->c.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
 http.addFilterBefore(new OncePerRequestFilter(){@Override protected void doFilterInternal(HttpServletRequest q,HttpServletResponse r,FilterChain c)throws ServletException,IOException{
 q.setAttribute("traceId",UUID.randomUUID().toString());r.setHeader("X-Trace-Id",String.valueOf(q.getAttribute("traceId")));var session=q.getSession(false);
 if(session!=null&&session.getAttribute("uid")!=null){long uid=((Number)session.getAttribute("uid")).longValue();var u=store.user(uid);if(u==null||!Boolean.TRUE.equals(u.get("enabled"))||!Objects.equals(Jsons.integer(u.get("sessionVersion")),session.getAttribute("sessionVersion"))){session.invalidate();}else{SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(uid,null,List.of()));String path=RolePermissions.path(q);if(!path.startsWith("/api/v1/auth/")&&path.startsWith("/api/")){var scopes=store.scopes(uid);var scopeId=session.getAttribute("scopeId");var scope=scopeId==null?null:store.scope(uid,((Number)scopeId).longValue());if(scope==null){write(r,403,"SCOPE_REQUIRED","请选择有效身份",json);return;}if(Boolean.TRUE.equals(u.get("mustChangePassword"))){write(r,403,"PASSWORD_CHANGE_REQUIRED","请先修改初始密码",json);return;}if(Auth.totpRequired(scope)&&!testAccess.allows(uid)&&!Boolean.TRUE.equals(session.getAttribute("totpVerified"))){write(r,403,"TOTP_REQUIRED","请先完成双因素验证",json);return;}}}}
 try{c.doFilter(q,r);}finally{SecurityContextHolder.clearContext();}
 }},org.springframework.security.web.csrf.CsrfFilter.class);
 return http.build();}
 static void write(HttpServletResponse r,int status,String code,String message,Jsons json)throws IOException{r.setStatus(status);r.setContentType("application/json;charset=UTF-8");r.getWriter().write(json.write(Api.error(code,message)));}
}
