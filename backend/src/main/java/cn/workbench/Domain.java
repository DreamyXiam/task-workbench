package cn.workbench;
import java.util.*;
public final class Domain {
 public static boolean companyAllowed(String role,Integer scopeCompany,int company){return "GROUP_LEADER".equals(role)?scopeCompany==null:Objects.equals(scopeCompany,company);}
 public static void version(int actual,int expected){if(actual!=expected)throw ApiException.conflict();}
 public static String transition(String status,String action){return switch(action){case "confirm-assign"->{require(status,"DRAFT");yield "IN_PROGRESS";}case "summarize"->{if(!Set.of("IN_PROGRESS","REWORK").contains(status))throw ApiException.conflict();yield "PENDING_ACCEPTANCE";}case "accept"->{if(!Set.of("PENDING_ACCEPTANCE","PENDING_ACCEPTANCE_WITH_GAP").contains(status))throw ApiException.conflict();yield "COMPLETED";}case "reject"->{if(!Set.of("PENDING_ACCEPTANCE","PENDING_ACCEPTANCE_WITH_GAP").contains(status))throw ApiException.conflict();yield "REWORK";}case "cancel"->{if(Set.of("COMPLETED","CANCELLED").contains(status))throw ApiException.conflict();yield "CANCELLED";}default->throw new ApiException(400,"INVALID_ACTION","未知操作");};}
 static void require(String value,String expected){if(!expected.equals(value))throw ApiException.conflict();}
 public static String csv(String value){String s=value==null?"":value; if(s.stripLeading().matches("^[=+@-].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
}
