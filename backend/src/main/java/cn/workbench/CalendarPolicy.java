package cn.workbench;

import java.math.*;
import java.time.*;
import java.util.*;
import static cn.workbench.Records.map;

/** Deterministic, simulation-only calendar policy. It never writes formal KPI. */
final class CalendarPolicy {
 static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
 static final BigDecimal HUNDRED=new BigDecimal("100");
 record Leave(int dept,Long user,Instant from,Instant to){}
 record Calendar(String mode,Set<Integer> weekends,LocalTime start,LocalTime end,Set<LocalDate> holidays,Set<LocalDate> working,List<Leave> leaves){}
 static Calendar baseline(){return new Calendar("NATURAL_TIME",Set.of(),LocalTime.MIN,LocalTime.MAX,Set.of(),Set.of(),List.of());}
 static Calendar validate(Map<String,Object> raw){
  try{
   String mode=String.valueOf(raw.getOrDefault("mode","WORKING_TIME"));if(!Set.of("WORKING_TIME","NATURAL_TIME").contains(mode))throw new IllegalArgumentException();
   Set<Integer> weekends=new TreeSet<>();for(Object v:list(raw.getOrDefault("weekendDays",List.of(0,6)))){int day=Jsons.integer(v);if(day<0||day>6)throw new IllegalArgumentException();weekends.add(day);}
   LocalTime start=LocalTime.parse(String.valueOf(raw.getOrDefault("dayStart","09:00"))),end=LocalTime.parse(String.valueOf(raw.getOrDefault("dayEnd","18:00")));if(!end.isAfter(start))throw new IllegalArgumentException();
   Set<LocalDate> holidays=dates(raw.getOrDefault("holidays",List.of())),working=dates(raw.getOrDefault("workingDates",List.of()));if(holidays.stream().anyMatch(working::contains))throw new IllegalArgumentException();
   List<Leave> leaves=new ArrayList<>();for(Object v:list(raw.getOrDefault("leaves",List.of()))){if(!(v instanceof Map<?,?> m))throw new IllegalArgumentException();int dept=Jsons.integer(m.get("deptId"));Long user=m.get("userId")==null?null:Jsons.number(m.get("userId"));Instant from=Records.instant(m.get("from")),to=Records.instant(m.get("to"));if(!to.isAfter(from)||Duration.between(from,to).toDays()>366)throw new IllegalArgumentException();leaves.add(new Leave(dept,user,from,to));}if(leaves.size()>1000)throw new IllegalArgumentException();
   return new Calendar(mode,Set.copyOf(weekends),start,end,Set.copyOf(holidays),Set.copyOf(working),List.copyOf(leaves));
  }catch(RuntimeException e){throw TaskService.invalid("日历配置无效；请检查日期、工作时间与请假区间");}
 }
 static List<?> list(Object v){if(!(v instanceof List<?> l)||l.size()>2000)throw new IllegalArgumentException();return l;}
 static Set<LocalDate> dates(Object v){Set<LocalDate> dates=new TreeSet<>();for(Object day:list(v))dates.add(LocalDate.parse(String.valueOf(day)));return dates;}
 static List<BigDecimal> weights(Object v){try{List<?> list=list(v);if(list.size()!=5)throw new IllegalArgumentException();var result=new ArrayList<BigDecimal>();for(var x:list){var n=new BigDecimal(x.toString());if(n.signum()<0||n.compareTo(HUNDRED)>0||n.scale()>4)throw new IllegalArgumentException();result.add(n);}if(result.stream().reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(HUNDRED)!=0)throw new IllegalArgumentException();return List.copyOf(result);}catch(RuntimeException e){throw TaskService.invalid("五项权重必须非负且合计100");}}
 static Map<String,BigDecimal> complexity(Object v){if(!(v instanceof Map<?,?> raw))throw TaskService.invalid("复杂度系数无效");Map<String,BigDecimal> result=new TreeMap<>();try{for(String key:List.of("SIMPLE","NORMAL","COMPLEX")){var n=new BigDecimal(String.valueOf(raw.get(key)));if(n.compareTo(new BigDecimal("0.25"))<0||n.compareTo(new BigDecimal("4"))>0||n.scale()>4)throw new IllegalArgumentException();result.put(key,n);}if(result.get("NORMAL").compareTo(BigDecimal.ONE)!=0)throw new IllegalArgumentException();}catch(RuntimeException e){throw TaskService.invalid("复杂度系数范围0.25至4，普通固定为1");}return result;}
 static boolean applies(Leave l,int dept,Long owner){return l.dept==dept&&(l.user==null||Objects.equals(l.user,owner));}
 static boolean onLeave(Instant deadline,Calendar c,int dept,Long owner){return c.leaves.stream().anyMatch(l->applies(l,dept,owner)&&!deadline.isBefore(l.from)&&deadline.isBefore(l.to));}
 static BigDecimal hours(Instant from,Instant to,Calendar c,int dept,Long owner){
  if(to.isBefore(from))throw TaskService.invalid("耗时终点早于起点");if(Duration.between(from,to).toDays()>3660)throw TaskService.invalid("模拟区间超过10年");
  long seconds=0;LocalDate first=from.atZone(ZONE).toLocalDate(),last=to.atZone(ZONE).toLocalDate();
  for(LocalDate day=first;!day.isAfter(last);day=day.plusDays(1)){
   boolean natural=c.mode.equals("NATURAL_TIME");if(!natural&&!c.working.contains(day)&&(c.weekends.contains(day.getDayOfWeek().getValue()%7)||c.holidays.contains(day)))continue;
   Instant begin=natural?day.atStartOfDay(ZONE).toInstant():day.atTime(c.start).atZone(ZONE).toInstant(),end=natural?day.plusDays(1).atStartOfDay(ZONE).toInstant():day.atTime(c.end).atZone(ZONE).toInstant();begin=begin.isBefore(from)?from:begin;end=end.isAfter(to)?to:end;if(!end.isAfter(begin))continue;
   final Instant a=begin,b=end;var exclusions=c.leaves.stream().filter(l->applies(l,dept,owner)&&l.to.isAfter(a)&&l.from.isBefore(b)).map(l->new Instant[]{l.from.isBefore(a)?a:l.from,l.to.isAfter(b)?b:l.to}).sorted(Comparator.comparing(x->x[0])).toList();
   long excluded=0;Instant eStart=null,eEnd=null;for(var interval:exclusions){if(eStart==null){eStart=interval[0];eEnd=interval[1];}else if(!interval[0].isAfter(eEnd)){if(interval[1].isAfter(eEnd))eEnd=interval[1];}else{excluded+=Duration.between(eStart,eEnd).toSeconds();eStart=interval[0];eEnd=interval[1];}}if(eStart!=null)excluded+=Duration.between(eStart,eEnd).toSeconds();seconds+=Duration.between(begin,end).toSeconds()-excluded;
  }
  return BigDecimal.valueOf(seconds).divide(new BigDecimal("3600"),10,RoundingMode.HALF_UP);
 }
 static Map<String,Object> score(List<Map<String,Object>> subs,Calendar calendar,List<BigDecimal> weights,Map<String,BigDecimal> factors,Map<Long,String> classifications){
  BigDecimal total=BigDecimal.ZERO,timely=BigDecimal.ZERO,overdue=BigDecimal.ZERO,accepted=BigDecimal.ZERO,rework=BigDecimal.ZERO;List<BigDecimal> response=new ArrayList<>(),execution=new ArrayList<>();int excluded=0;
  for(var s:subs){int dept=TaskService.integer(s,"deptId");Long owner=s.get("ownerId")==null?null:Jsons.number(s.get("ownerId"));Instant deadline=Records.instant(s.get("deadline"));if(onLeave(deadline,calendar,dept,owner)){excluded++;continue;}
   BigDecimal factor=factors.getOrDefault(classifications.getOrDefault(Jsons.number(s.get("taskId")),"NORMAL"),BigDecimal.ONE);total=total.add(factor);Instant submitted=s.get("submittedAt")==null?null:Records.instant(s.get("submittedAt"));if(submitted!=null&&!submitted.isAfter(deadline))timely=timely.add(factor);if(Boolean.TRUE.equals(s.get("everOverdue"))||(submitted==null||submitted.isAfter(deadline)))overdue=overdue.add(factor);
   if("ACCEPTED".equals(s.get("status"))){accepted=accepted.add(factor);if(s.get("rejectCount")!=null&&Jsons.integer(s.get("rejectCount"))>0)rework=rework.add(factor);}
   if(s.get("firstReceivedAt")!=null){Instant received=Records.instant(s.get("firstReceivedAt"));Object dispatched=s.getOrDefault("dispatchedAt",s.get("createdAt"));if(dispatched!=null)response.add(hours(Records.instant(dispatched),received,calendar,dept,owner).divide(factor,10,RoundingMode.HALF_UP));if(submitted!=null)execution.add(hours(received,submitted,calendar,dept,owner).divide(factor,10,RoundingMode.HALF_UP));}
  }
  var metrics=map("timelyRate",Kpi.ratio(timely,total),"overdueRate",Kpi.ratio(overdue,total),"reworkRate",Kpi.ratio(rework,accepted),"responseHours",KpiController.average(response),"executionHours",KpiController.average(execution),"denominator",subs.size()-excluded,"weightedDenominator",total,"excludedLeaveCount",excluded);
  var indicators=Kpi.evaluate(KpiController.decimal(metrics.get("timelyRate")),KpiController.decimal(metrics.get("overdueRate")),KpiController.decimal(metrics.get("reworkRate")),KpiController.decimal(metrics.get("responseHours")),KpiController.decimal(metrics.get("executionHours")));
  BigDecimal score=BigDecimal.ZERO;for(int i=0;i<5;i++){if(indicators.get(i)==null&&weights.get(i).signum()>0){score=null;break;}if(indicators.get(i)!=null)score=score.add(indicators.get(i).multiply(weights.get(i)));}if(score!=null)score=score.divide(HUNDRED,2,RoundingMode.HALF_UP);
  return map("total",score,"indicatorScores",indicators,"metrics",metrics);
 }
 static Map<String,Object> correlation(List<Map<String,Object>> rows){List<double[]> samples=new ArrayList<>();for(var row:rows){var baseline=(Map<?,?>)row.get("baseline");var metrics=(Map<?,?>)baseline.get("metrics");Object x=metrics.get("timelyRate"),y=metrics.get("overdueRate");if(x!=null&&y!=null)samples.add(new double[]{Double.parseDouble(x.toString()),Double.parseDouble(y.toString())});}Double coefficient=null;if(samples.size()>=3){double ax=samples.stream().mapToDouble(s->s[0]).average().orElse(0),ay=samples.stream().mapToDouble(s->s[1]).average().orElse(0),xx=0,yy=0,xy=0;for(var s:samples){double x=s[0]-ax,y=s[1]-ay;xx+=x*x;yy+=y*y;xy+=x*y;}if(xx>0&&yy>0)coefficient=xy/Math.sqrt(xx*yy);}return map("sampleCount",samples.size(),"coefficient",coefficient==null?null:BigDecimal.valueOf(coefficient).setScale(4,RoundingMode.HALF_UP),"warning",coefficient==null?"样本不足或方差为零，无法判断相关性；不自动调整正式权重":"按时率与逾期率可能重复反映同一事实；仅供分析，不自动调整正式权重");}
}
