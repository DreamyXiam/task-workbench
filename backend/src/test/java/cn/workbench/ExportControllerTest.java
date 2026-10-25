package cn.workbench;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:exports;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","task.bootstrap-password=TestOnly-Synthetic12345","task.totp-key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="})
@org.springframework.test.context.TestExecutionListeners(value={org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,org.springframework.test.context.transaction.TransactionalTestExecutionListener.class},mergeMode=org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@ActiveProfiles("test") @Transactional class ExportControllerTest {
 @Autowired StatsController stats;
 MockHttpServletRequest request(long uid,long scope,boolean fresh){var q=new MockHttpServletRequest();q.getSession().setAttribute("uid",uid);q.getSession().setAttribute("scopeId",scope);if(fresh)q.getSession().setAttribute("totpAt",System.currentTimeMillis());return q;}
 @Test void exportRequiresFreshTotp(){assertThatThrownBy(()->stats.export(request(2,2,false),"xlsx",null,null)).isInstanceOf(ApiException.class).extracting("code").isEqualTo("FRESH_TOTP_REQUIRED");}
 @Test void employeeCannotExportEvenWithFreshTotp(){assertThatThrownBy(()->stats.export(request(4,4,true),"xlsx",null,null)).isInstanceOf(ApiException.class);}
 @Test void xlsxIsActualWorkbookAndRepeatedRequestIsRateLimited(){var q=request(2,2,true);var response=stats.export(q,"xlsx",null,null);assertThat(response.getHeaders().getContentType().toString()).contains("spreadsheetml");assertThat(response.getBody()).startsWith((byte)'P',(byte)'K');assertThatThrownBy(()->stats.export(q,"xlsx",null,null)).isInstanceOf(ApiException.class).extracting("code").isEqualTo("EXPORT_RATE_LIMIT");}
}
