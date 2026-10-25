package cn.workbench;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.math.*;
import java.util.*;
import static cn.workbench.Records.map;
import static org.assertj.core.api.Assertions.*;
class CalendarPolicyTest {
 Map<String,Object> calendar(){return map("mode","WORKING_TIME","weekendDays",List.of(0,6),"dayStart","09:00","dayEnd","18:00","holidays",List.of("2026-10-01"),"workingDates",List.of("2026-10-03"),"leaves",List.of());}
 Instant at(String s){return OffsetDateTime.parse(s+"+08:00").toInstant();}
 @Test void calendarCrossesMonthsAndSkipsHolidayButIncludesMakeupDay(){
  var c=CalendarPolicy.validate(calendar());
  assertThat(CalendarPolicy.hours(at("2026-09-30T09:00:00"),at("2026-10-03T18:00:00"),c,3,4L)).isEqualByComparingTo("27");
 }
 @Test void overlappingLeaveIntervalsAreRemovedOnce(){var input=calendar();input.put("leaves",List.of(map("deptId",3,"from","2026-10-02T10:00:00+08:00","to","2026-10-02T14:00:00+08:00"),map("deptId",3,"userId",4,"from","2026-10-02T12:00:00+08:00","to","2026-10-02T16:00:00+08:00")));var c=CalendarPolicy.validate(input);assertThat(CalendarPolicy.hours(at("2026-10-02T09:00:00"),at("2026-10-02T18:00:00"),c,3,4L)).isEqualByComparingTo("3");assertThat(CalendarPolicy.hours(at("2026-10-02T09:00:00"),at("2026-10-02T18:00:00"),c,4,4L)).isEqualByComparingTo("9");}
 @Test void noDeadlineSamplesRemainNotApplicable(){var score=CalendarPolicy.score(List.of(),CalendarPolicy.validate(calendar()),Kpi.WEIGHTS,Map.of("NORMAL",BigDecimal.ONE),Map.of());assertThat(score.get("total")).isNull();}
 @Test void deadlinesInsideLeaveAreExplicitlyExcluded(){var input=calendar();input.put("leaves",List.of(map("deptId",3,"from","2026-10-02T10:00:00+08:00","to","2026-10-02T16:00:00+08:00")));var sub=map("id",100,"taskId",10,"deptId",3,"ownerId",4,"deadline","2026-10-02T12:00:00+08:00","status","ACCEPTED","submittedAt","2026-10-02T11:00:00+08:00","rejectCount",0);var score=CalendarPolicy.score(List.of(sub),CalendarPolicy.validate(input),Kpi.WEIGHTS,Map.of("NORMAL",BigDecimal.ONE),Map.of());assertThat(score.get("total")).isNull();assertThat(((Map<?,?>)score.get("metrics")).get("excludedLeaveCount")).isEqualTo(1);}
 @Test void candidateWeightsAreNotFormalWeights(){assertThat(Kpi.WEIGHTS).containsExactly(new BigDecimal("30"),new BigDecimal("25"),new BigDecimal("20"),new BigDecimal("10"),new BigDecimal("15"));assertThatThrownBy(()->CalendarPolicy.weights(List.of(10,10,10,10,10))).isInstanceOf(ApiException.class);}
 @Test void unrelatedDepartmentLeaveDoesNotExcludeTask(){var input=calendar();input.put("leaves",List.of(map("deptId",4,"from","2026-10-02T10:00:00+08:00","to","2026-10-02T16:00:00+08:00")));var c=CalendarPolicy.validate(input);assertThat(CalendarPolicy.onLeave(at("2026-10-02T12:00:00"),c,3,4L)).isFalse();}
}
