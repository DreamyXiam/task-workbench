package cn.workbench;
import java.math.*;import java.util.*;
public final class Kpi {
 public static final List<BigDecimal> WEIGHTS=List.of(new BigDecimal("30"),new BigDecimal("25"),new BigDecimal("20"),new BigDecimal("10"),new BigDecimal("15"));
 public static List<BigDecimal> evaluate(BigDecimal timely,BigDecimal overdue,BigDecimal returned,BigDecimal responseHours,BigDecimal executionHours){return Arrays.asList(indicator(timely,"70","90",true),indicator(overdue,"20","5",false),indicator(returned,"15","5",false),indicator(responseHours,"8","2",false),indicator(executionHours,"24","12",false));}
 public static BigDecimal indicator(BigDecimal value,String baseline,String target,boolean higher){if(value==null)return null;BigDecimal b=new BigDecimal(baseline),t=new BigDecimal(target);BigDecimal n=higher?value.subtract(b):b.subtract(value),d=higher?t.subtract(b):b.subtract(t);return n.multiply(new BigDecimal("100")).divide(d,10,RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(new BigDecimal("100"));}
 public static BigDecimal calculate(List<BigDecimal> scores){if(scores.size()!=5)throw new IllegalArgumentException("five indicators required");BigDecimal total=BigDecimal.ZERO;for(int i=0;i<5;i++){if(scores.get(i)==null)return null;total=total.add(scores.get(i).max(BigDecimal.ZERO).min(new BigDecimal("100")).multiply(WEIGHTS.get(i)));}return total.divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);}
 public static BigDecimal ratio(BigDecimal n,BigDecimal d){return d.signum()==0?null:n.multiply(new BigDecimal("100")).divide(d,2,RoundingMode.HALF_UP);}
}
