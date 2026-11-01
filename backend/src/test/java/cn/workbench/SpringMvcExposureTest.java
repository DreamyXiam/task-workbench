package cn.workbench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.view.xslt.XsltView;
import org.springframework.web.servlet.view.xslt.XsltViewResolver;
import static org.assertj.core.api.Assertions.assertThat;

/** Guards the application configuration assumptions behind the scoped Spring MVC VEX. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:exposure;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@TestExecutionListeners(value = {
    org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
    org.springframework.test.context.transaction.TransactionalTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class SpringMvcExposureTest {
    private static final String TEST_PASSWORD = "ExposureCheck-" + UUID.randomUUID() + "A1";
    @DynamicPropertySource
    static void testCredentials(DynamicPropertyRegistry registry) {
        registry.add("task.bootstrap-password", () -> TEST_PASSWORD);
        registry.add("task.totp-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

    @Test void applicationHandlersRemainRestOnlyWithoutXsltViewsOrSseFragments() throws Exception {
        assertThat(org.springframework.core.SpringVersion.getVersion()).isEqualTo("6.2.19");
        assertThat(java.time.LocalDate.now(java.time.ZoneOffset.UTC))
            .as("VEX must be reviewed by 2026-12-01").isBefore(java.time.LocalDate.of(2026, 12, 2));
        assertThat(context.getBeansOfType(XsltView.class)).isEmpty();
        assertThat(context.getBeansOfType(XsltViewResolver.class)).isEmpty();
        var handlers = mappings.getHandlerMethods().entrySet().stream()
            .filter(e -> e.getValue().getBeanType().getPackageName().equals("cn.workbench")
                || e.getValue().getBeanType().getPackageName().startsWith("cn.workbench.")).toList();
        assertThat(handlers).isNotEmpty();
        for (var entry : handlers) {
            var method = entry.getValue();
            assertThat(AnnotatedElementUtils.hasAnnotation(method.getBeanType(), RestController.class))
                .as("Response-body semantics for %s", method).isTrue();
            assertThat(entry.getKey().getProducesCondition().getProducibleMediaTypes())
                .noneMatch(t -> t.isCompatibleWith(org.springframework.http.MediaType.TEXT_EVENT_STREAM));
            assertThat(method.getReturnType().getGenericParameterType().getTypeName())
                .doesNotContain("SseEmitter", "ResponseBodyEmitter", "FragmentsRendering", "ModelAndView");
        }
        // Also guard dynamically typed Object returns/configuration against direct feature introduction.
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (var path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertThat(Files.readString(path)).as("Reassess VEX when changing %s", path)
                    .doesNotContain("XsltView", "SseEmitter", "ResponseBodyEmitter", "FragmentsRendering", "ServerSentEvent", "text/event-stream");
            }
        }
    }
}
