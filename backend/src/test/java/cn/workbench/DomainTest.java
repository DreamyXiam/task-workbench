package cn.workbench;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class DomainTest {
 @Test void rawGoldenMetrics(){ assertThat(Kpi.evaluate(new BigDecimal("92"),new BigDecimal("8"),new BigDecimal("3"),new BigDecimal("3"),new BigDecimal("18"))).map(x->x.setScale(2,java.math.RoundingMode.HALF_UP)).containsExactly(new BigDecimal("100.00"),new BigDecimal("80.00"),new BigDecimal("100.00"),new BigDecimal("83.33"),new BigDecimal("50.00")); }
 @Test void goldenKpiAndMissingDenominator(){ assertThat(Kpi.calculate(List.of(new BigDecimal("100"),new BigDecimal("80"),new BigDecimal("100"),new BigDecimal("83.33"),new BigDecimal("50")))).isEqualByComparingTo("85.83"); assertThat(Kpi.ratio(BigDecimal.ONE,BigDecimal.ZERO)).isNull(); }
 @Test void invalidStateAndStaleVersionFail(){ assertThatThrownBy(()->Domain.transition("COMPLETED","summarize")).isInstanceOf(ApiException.class); assertThatThrownBy(()->Domain.version(3,2)).isInstanceOf(ApiException.class); assertThat(Domain.transition("PENDING_ACCEPTANCE","reject")).isEqualTo("REWORK"); }
 @Test void totpRfcVectorAndReplayWindow(){ assertThat(Totp.code("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",59L/30)).isEqualTo("287082"); assertThat(Totp.verify("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ","287082",Instant.ofEpochSecond(59),-1)).isEqualTo(1); assertThat(Totp.verify("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ","287082",Instant.ofEpochSecond(59),1)).isEqualTo(-1); }
 @Test void csvFormulaInjectionIsEscaped(){ assertThat(Domain.csv("=SUM(1,2)")).isEqualTo("\"'=SUM(1,2)\""); assertThat(Domain.csv(" @evil")).startsWith("\"'"); }
 @Test void scopeDoesNotMergeCompanies(){ assertThat(Domain.companyAllowed("EMPLOYEE",1,2)).isFalse(); assertThat(Domain.companyAllowed("ADMIN",1,2)).isFalse(); assertThat(Domain.companyAllowed("GROUP_LEADER",null,2)).isTrue(); }
}
