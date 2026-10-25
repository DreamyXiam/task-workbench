package cn.workbench;
import java.util.*;
public final class ProjectionSql {
 private static final Set<String> TABLES=Set.of("company","department","task","sub_task","task_comment","task_deadline_history","task_progress","deadline_request","collaboration_request","result_version","function_tag","matching_rule","matching_feedback","notification_channel","kpi_template","kpi_run","kpi_score","kpi_appeal","kpi_adjustment","attachment");
 public static String insert(Map<String,Object> args){String table=(String)args.get("table");if(!TABLES.contains(table))throw new IllegalArgumentException();var values=(Map<String,Object>)args.get("values");return "INSERT INTO "+table+" ("+String.join(",",values.keySet())+") VALUES ("+String.join(",",values.keySet().stream().map(k->"#{values."+k+"}").toList())+")";}
 public static String update(Map<String,Object> args){String table=(String)args.get("table");if(!TABLES.contains(table))throw new IllegalArgumentException();var values=(Map<String,Object>)args.get("values");return "UPDATE "+table+" SET "+String.join(",",values.keySet().stream().filter(k->!k.equals("id")).map(k->k+"=#{values."+k+"}").toList())+" WHERE id=#{values.id}";}
 public static String select(Map<String,Object> args){String table=(String)args.get("table");if(!TABLES.contains(table))throw new IllegalArgumentException();return "SELECT * FROM "+table+" WHERE id=#{id}";}
}
