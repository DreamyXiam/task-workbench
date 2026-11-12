package cn.workbench;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class AuditActionLabelsTest {
 @Test void emittedWorkflowCodesHaveHumanReadableLabels(){assertThat(AuditLogService.label("SUBTASK_RECEIVE")).isEqualTo("接收任务");assertThat(AuditLogService.label("CONFIRM-ASSIGN")).isEqualTo("确认任务分配");assertThat(AuditLogService.label("DEADLINE_CHANGED")).isEqualTo("调整截止日期");assertThat(AuditLogService.label("SUBTASK_PROGRESS")).isEqualTo("更新任务进展");}
 @Test void employeeCatalogDoesNotAdvertiseCollaborationApproval(){assertThat(RolePermissions.supported("EMPLOYEE")).doesNotContain("COLLABORATION");}
}
