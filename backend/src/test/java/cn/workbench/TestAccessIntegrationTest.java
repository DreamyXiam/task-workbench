package cn.workbench;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"task.bootstrap-password=TestOnly-Synthetic12345","task.totp-key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=","TASK_TEST_NO_2FA_IDS=1"})
@ActiveProfiles({"test","test-access"}) @AutoConfigureMockMvc
class TestAccessIntegrationTest {
 @DynamicPropertySource static void expiry(DynamicPropertyRegistry registry){registry.add("TASK_TEST_NO_2FA_EXPIRES_AT",()->Instant.now().plusSeconds(86400).toString());}
 @Autowired MockMvc mvc;@Autowired Store db;@Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
 MockHttpSession session(String account){var user=db.userByAccount(account);long id=Jsons.number(user.get("id"));db.password(id,passwords.encode("Fixture-Password123"));user=db.user(id);var session=new MockHttpSession();session.setAttribute("uid",id);session.setAttribute("sessionVersion",Jsons.integer(user.get("sessionVersion")));session.setAttribute("scopeId",Jsons.number(db.scopes(id).getFirst().get("id")));session.setAttribute("totpVerified",false);return session;}
 @Test @Transactional void onlyAllowlistedTestAccountBypassesTotpAndFreshChallenge()throws Exception{
  var admin=session("demo.admin");assertThat(admin.getAttribute("uid")).isEqualTo(1L);
  mvc.perform(get("/api/v1/auth/me").session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.testAccess").value(true)).andExpect(jsonPath("$.data.totpRequired").value(false));
  mvc.perform(post("/api/v1/admin/departments").session(admin).with(csrf()).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content("{\"companyId\":1,\"name\":\"Fixture department\",\"code\":\"FIXTURE\"}")).andExpect(status().isOk());
  var leader=session("demo.leader");
  mvc.perform(get("/api/v1/auth/me").session(leader)).andExpect(status().isOk()).andExpect(jsonPath("$.data.testAccess").value(false)).andExpect(jsonPath("$.data.totpRequired").value(true));
  mvc.perform(get("/api/v1/tasks").session(leader)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TOTP_REQUIRED"));
 }
}
