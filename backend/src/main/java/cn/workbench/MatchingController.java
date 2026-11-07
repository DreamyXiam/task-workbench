package cn.workbench;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/matching")
public class MatchingController {
    private final Records records;
    private final Policy policy;
    private final Writes writes;
    public MatchingController(Records records,Policy policy,Writes writes){this.records=records;this.policy=policy;this.writes=writes;}

    @GetMapping("/results/{id}")
    public Object result(@PathVariable long id,HttpServletRequest request){
        var actor=policy.actor(request);
        var saved=authorizedResult(actor,id);
        Map<String,Object> response=new LinkedHashMap<>(saved);
        response.remove("inputSnapshot");response.remove("normalizedTitle");response.remove("normalizedContent");
        return Api.ok(response);
    }

    @PostMapping("/feedback")
    public Object submit(@RequestBody Map<String,Object> body,HttpServletRequest request){
        var actor=policy.actor(request);
        long taskId=TaskService.number(body,"taskId"),resultId=TaskService.number(body,"matchingResultId");
        var task=policy.task(actor,taskId);
        policy.taskLeader(actor,Jsons.integer(task.get("companyId")));
        if(!actor.role().equals("GROUP_LEADER")&&Jsons.number(task.get("creatorId"))!=actor.uid())throw ApiException.denied();
        authorizedResult(actor,resultId);
        String reason=TaskService.text(body,"reason",1000);
        return writes.run(request,body,()->{
            var locked=records.lock(taskId);
            var matched=authorizedResult(actor,resultId);
            if(!Objects.equals(Jsons.integer(locked.get("companyId")),Jsons.integer(matched.get("companyId"))))throw ApiException.denied();
            String expected=MatchingEngine.inputHash(Jsons.integer(locked.get("companyId")),String.valueOf(locked.get("title")),String.valueOf(locked.get("content")));
            if(!expected.equals(matched.get("inputHash")))throw new ApiException(409,"MATCHING_INPUT_CHANGED","The suggestion does not match this task input; request a new suggestion.");
            var existing=records.children(taskId,"MATCHING_FEEDBACK").stream()
                    .filter(item->Jsons.number(item.get("matchingResultId"))==resultId).findFirst();
            if(existing.isPresent())return existing.get();
            int actualHost=Jsons.integer(locked.get("hostDeptId"));
            List<Long> actualCollaborators=integerList(locked.getOrDefault("collaboratorDeptIds",List.of()));
            List<Map<String,Object>> suggestions=maps(matched.get("suggestions"));
            var recommendedHost=suggestions.stream().filter(item->"HOST".equals(item.get("role"))).findFirst();
            List<Long> recommendedCollaborators=suggestions.stream().filter(item->"COLLABORATOR".equals(item.get("role")))
                    .limit(2).map(item->Jsons.number(item.get("deptId"))).sorted().toList();
            boolean accepted=recommendedHost.isPresent()&&Jsons.number(recommendedHost.get().get("deptId"))==actualHost
                    &&actualCollaborators.equals(recommendedCollaborators);
            var saved=records.create("MATCHING_FEEDBACK",Jsons.integer(locked.get("companyId")),null,taskId,actor.uid(),"RECORDED",
                    Records.map("matchingResultId",resultId,"reason",reason,"decision",accepted?"ACCEPTED":"OVERRIDDEN",
                            "actualHostDeptId",actualHost,"actualCollaboratorDeptIds",actualCollaborators,
                            "suggestedAssignments",suggestions,"ruleVersion",matched.get("ruleVersion"),
                            "inputHash",expected,"recordedBy",actor.uid()));
            writes.audit(actor,"MATCHING_FEEDBACK_"+(accepted?"ACCEPTED":"OVERRIDDEN"),Jsons.number(saved.get("id")));
            return saved;
        });
    }

    @GetMapping("/feedback")
    public Object list(@RequestParam(required=false)Integer companyId,@RequestParam(defaultValue="1")int page,
            @RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String from,@RequestParam(required=false)String to,HttpServletRequest request){
        var actor=policy.actor(request);
        Integer company=companyId==null?actor.company():companyId;
        if(company==null)throw new ApiException(400,"COMPANY_REQUIRED","Select one company for matching statistics.");
        policy.leader(actor,company);
        Instant start=bound(from),end=bound(to);
        if(start!=null&&end!=null&&!start.isBefore(end))throw new ApiException(400,"INVALID_RANGE","From must precede the exclusive To bound.");
        var items=records.list("MATCHING_FEEDBACK",company,null).stream()
                .filter(item->start==null||!Records.instant(item.get("createdAt")).isBefore(start))
                .filter(item->end==null||Records.instant(item.get("createdAt")).isBefore(end)).filter(item->{
            try{policy.task(actor,Jsons.number(item.get("taskId")));return true;}catch(ApiException denied){return false;}
        }).toList();
        long accepted=items.stream().filter(item->"ACCEPTED".equals(item.get("decision"))).count();
        BigDecimal rate=items.isEmpty()?null:BigDecimal.valueOf(accepted).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(items.size()),2,RoundingMode.HALF_UP);
        var response=new LinkedHashMap<>(TaskController.page(items,page,pageSize));
        response.put("summary",Records.map("sampleCount",items.size(),"accepted",accepted,"overridden",items.size()-accepted,
                "adoptionRate",rate,"label","Suggestion adoption rate; this does not measure matching accuracy."));
        return Api.ok(response);
    }

    private static Instant bound(String value){
        if(value==null||value.isBlank())return null;
        try{return Instant.parse(value);}catch(java.time.format.DateTimeParseException ignored){
            try{return LocalDate.parse(value).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();}
            catch(java.time.format.DateTimeParseException invalid){throw new ApiException(400,"INVALID_RANGE","Use ISO8601 timestamps or YYYY-MM-DD dates for From/To.");}
        }
    }

    private Map<String,Object> authorizedResult(Policy.Actor actor,long id){
        var result=records.get(id);
        if(!"MATCHING_RESULT".equals(result.get("kind")))throw ApiException.denied();
        policy.taskLeader(actor,Jsons.integer(result.get("companyId")));
        if(!actor.role().equals("GROUP_LEADER")&&(result.get("ownerId")==null||Jsons.number(result.get("ownerId"))!=actor.uid()))throw ApiException.denied();
        return result;
    }
    private static List<Long> integerList(Object value){if(!(value instanceof List<?> list))throw TaskService.invalid("Invalid department list");return list.stream().map(Jsons::number).sorted().toList();}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> maps(Object value){if(!(value instanceof List<?>))throw new IllegalStateException("Missing matching snapshot");return (List<Map<String,Object>>)value;}
}
