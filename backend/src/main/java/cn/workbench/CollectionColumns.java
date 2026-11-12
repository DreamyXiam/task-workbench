package cn.workbench;

import java.util.*;
import static cn.workbench.Records.map;

final class CollectionColumns {
 static final List<String> IDS=List.of("content","progress","problem","suggestion","coordination","timeRequirement","remark");
 static final List<String> NAMES=List.of("工作内容","推进情况","存在问题","解决建议","需要领导协调解决工作","时间要求","备注");
 static List<Map<String,Object>> defaults(){var result=new ArrayList<Map<String,Object>>();for(int i=0;i<IDS.size();i++)result.add(map("id",IDS.get(i),"name",NAMES.get(i)));return result;}
 static List<Map<String,Object>> read(Object raw){return raw==null?defaults():validate(raw);}
 static List<Map<String,Object>> validate(Object raw){
  if(!(raw instanceof List<?> list)||list.isEmpty()||list.size()>18)throw TaskService.invalid("请选择1至18个填报列");
  var result=new ArrayList<Map<String,Object>>();var ids=new HashSet<String>();var names=new HashSet<String>();
  for(Object item:list){
   if(!(item instanceof Map<?,?> column)||column.size()!=2||!(column.get("id") instanceof String id)||!(column.get("name") instanceof String name))throw TaskService.invalid("填报列格式无效");
   boolean standard=IDS.contains(id),custom=id.matches("custom_[0-9a-f]{32}");
   String clean=name.strip();
   if(!standard&&!custom||clean.isEmpty()||clean.length()>40||clean.codePoints().anyMatch(c->c<32)||!ids.add(id)||!names.add(clean.toLowerCase(Locale.ROOT)))throw TaskService.invalid("填报列名称或标识无效、重复");
   result.add(map("id",id,"name",clean));
  }
  return result;
 }
 static List<String> ids(List<Map<String,Object>> columns){return columns.stream().map(c->(String)c.get("id")).toList();}
 static List<String> names(List<Map<String,Object>> columns){return columns.stream().map(c->(String)c.get("name")).toList();}
}
