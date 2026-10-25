package cn.workbench;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Deterministic suggestions only: this class never assigns a department or changes a rule. */
public final class MatchingEngine {
    public static final String ENGINE_VERSION = "v1.5-matching-1";
    private static final Pattern SPACE = Pattern.compile("[\\s\\p{Z}]+");
    private static final Pattern FORMAT = Pattern.compile("\\p{Cf}");
    private static final Pattern NEGATION = Pattern.compile("(?:不需要|不涉及|不要|无需|无须|禁止|不|非|\\b(?:without|no|not))\\s*$");

    public static String normalize(String value) {
        if (value == null) return "";
        return SPACE.matcher(FORMAT.matcher(Normalizer.normalize(value, Normalizer.Form.NFKC)).replaceAll(""))
                .replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }

    public static String inputHash(int companyId,String title,String content) {
        return hash(companyId+"\n"+normalize(title)+"\n"+normalize(content));
    }

    public Map<String,Object> match(int companyId,String title,String content,
            List<Map<String,Object>> departments,List<Map<String,Object>> tags,List<Map<String,Object>> rules) {
        if (companyId <= 0 || title == null || title.length()>300 || content==null || content.length()>20000)
            throw new IllegalArgumentException("Invalid matching input");
        String normalizedTitle=normalize(title), normalizedContent=normalize(content);
        String text=normalizedTitle+"\n"+normalizedContent;
        Map<Long,String> allowedDepartments=new TreeMap<>();
        for (var department:departments) {
            if (integer(department.get("companyId"),0)==companyId && active(department))
                allowedDepartments.put(number(department.get("id")),String.valueOf(department.get("name")));
        }
        Map<String,Source> effective=new TreeMap<>();
        collect(effective,"TAG",companyId,tags,allowedDepartments.keySet());
        collect(effective,"RULE",companyId,rules,allowedDepartments.keySet());
        List<Map<String,Object>> snapshots=new ArrayList<>(), excluded=new ArrayList<>();
        Map<Long,Candidate> candidates=new TreeMap<>();
        for (Source source:effective.values()) {
            snapshots.add(source.snapshot());
            if (!source.enabled) continue;
            String negative=source.negativeKeywords.stream().filter(term->contains(text,term)).findFirst().orElse(null);
            if (negative!=null) {
                excluded.add(source.hit(negative,0,"NEGATIVE_RULE", "EXCLUDED",-1));
                continue;
            }
            for (String term:source.fixedPhrases) {
                int offset=affirmativeOffset(text,term,source,excluded,"FIXED_PHRASE");
                if(offset<0)continue;
                boolean exact=normalizedTitle.equals(term)||normalizedContent.equals(term);
                add(candidates,source,allowedDepartments.get(source.deptId),term,
                        exact?3:2,term.codePointCount(0,term.length()),exact?"EXACT_FIXED":"FIXED_PHRASE",offset);
            }
            for (String term:source.keywords) {
                int offset=affirmativeOffset(text,term,source,excluded,"KEYWORD");
                if(offset>=0)add(candidates,source,allowedDepartments.get(source.deptId),term,1,0,"KEYWORD",offset);
            }
        }
        Comparator<Candidate> ranking=Comparator.comparingInt((Candidate c)->c.tier).reversed()
                .thenComparing(Comparator.comparingInt((Candidate c)->c.fixedLength).reversed())
                .thenComparing(Comparator.comparingInt((Candidate c)->c.priority).reversed())
                .thenComparing(Comparator.comparingInt((Candidate c)->c.score).reversed())
                .thenComparingLong(c->c.deptId);
        List<Candidate> ranked=candidates.values().stream().filter(c->c.score>0).sorted(ranking).toList();
        Candidate host=ranked.stream().filter(c->c.hostEligible).findFirst().orElse(null);
        List<Long> ties=host==null?List.of():ranked.stream().filter(c->c.hostEligible&&c.sameRank(host)).map(c->c.deptId).toList();
        List<Map<String,Object>> suggestions=new ArrayList<>();
        for(Candidate candidate:ranked) {
            List<String> reasons=new ArrayList<>();
            for(var hit:candidate.hits)reasons.add(("EXACT_FIXED".equals(hit.get("matchType"))?"固定规则精确匹配":
                    "FIXED_PHRASE".equals(hit.get("matchType"))?"固定短语命中":"关键词命中")+"："+hit.get("term")
                    +"；权重 "+hit.get("weight")+"；"+hit.get("sourceKind")+"#"+hit.get("sourceId")+" 规则版本 "+hit.get("ruleVersion"));
            if(ties.size()>1&&ties.contains(candidate.deptId))reasons.add("同分主办候选，请人工确认唯一主办部门。");
            reasons.add("匹配仅供参考，须由领导确认最终分配。");
            suggestions.add(map("deptId",candidate.deptId,"deptName",candidate.deptName,
                    "role",candidate==host?"HOST":"COLLABORATOR","score",candidate.score,
                    "reasons",reasons,"hits",candidate.hits,"hostCandidate",ties.contains(candidate.deptId),
                    "requiresConfirmation",true));
        }
        String version=ENGINE_VERSION+"-"+hash(canonical(snapshots)).substring(0,16);
        return freeze(map("suggestions",suggestions,"ruleVersion",version,"engineVersion",ENGINE_VERSION,
                "manualFallback",host==null,"tiedHostDeptIds",ties,"excludedHits",excluded,
                "message",host==null?"未生成主办建议，请人工选择部门。":"请人工确认唯一主办部门和零至两个协办部门。",
                "inputHash",inputHash(companyId,title,content),"normalizedTitle",normalizedTitle,
                "normalizedContent",normalizedContent,"ruleSnapshot",snapshots));
    }

    private static void collect(Map<String,Source> result,String kind,int companyId,List<Map<String,Object>> rows,Set<Long> allowed) {
        Map<String,List<Source>> grouped=new TreeMap<>();
        for(var row:rows) {
            int scope=integer(row.get("companyId"),0);
            if(scope!=0&&scope!=companyId || row.get("deptId")==null)continue;
            long deptId=number(row.get("deptId"));
            if(!allowed.contains(deptId))continue;
            String name=normalize(String.valueOf(row.getOrDefault("name","")));
            String logical=kind+":"+normalize(String.valueOf(row.getOrDefault("code",name)));
            grouped.computeIfAbsent(logical,key->new ArrayList<>()).add(new Source(kind,logical,row,deptId,scope));
        }
        for(var group:grouped.entrySet()) {
            int scope=group.getValue().stream().mapToInt(source->source.scope).max().orElse(0);
            for(Source source:group.getValue()) {
                if(source.scope!=scope)continue;
                String key=group.getKey()+":"+source.deptId;
                Source previous=result.get(key);
                if(previous==null||source.ruleVersion>previous.ruleVersion||source.ruleVersion==previous.ruleVersion&&source.id>previous.id)
                    result.put(key,source);
            }
        }
    }

    private static void add(Map<Long,Candidate> candidates,Source source,String deptName,String term,int tier,int length,String type,int offset) {
        if(source.weight==0)return;
        Candidate candidate=candidates.computeIfAbsent(source.deptId,id->new Candidate(id,deptName));
        candidate.score=Math.addExact(candidate.score,source.weight);
        int stronger=Integer.compare(tier,candidate.tier);
        if(stronger==0)stronger=Integer.compare(length,candidate.fixedLength);
        if(stronger==0)stronger=Integer.compare(source.priority,candidate.priority);
        if(stronger>0){candidate.tier=tier;candidate.fixedLength=length;candidate.priority=source.priority;candidate.hostEligible=!source.role.equals("COLLABORATOR");}
        else if(stronger==0)candidate.hostEligible|=!source.role.equals("COLLABORATOR");
        candidate.hits.add(source.hit(term,source.weight,type,"MATCHED",offset));
    }

    private static int affirmativeOffset(String text,String term,Source source,List<Map<String,Object>> excluded,String type) {
        int start=0;
        while(start<=text.length()-term.length()) {
            int offset=text.indexOf(term,start);
            if(offset<0)return -1;
            start=offset+Math.max(1,term.length());
            if(!boundary(text,term,offset))continue;
            String prefix=text.substring(Math.max(0,offset-16),offset);
            if(NEGATION.matcher(prefix).find())excluded.add(source.hit(term,0,type,"NEGATED",offset));
            else return offset;
        }
        return -1;
    }

    private static boolean contains(String text,String term) {
        for(int from=0;from<=text.length()-term.length();) {
            int offset=text.indexOf(term,from);if(offset<0)return false;
            if(boundary(text,term,offset))return true;
            from=offset+Math.max(1,term.length());
        }
        return false;
    }

    private static boolean boundary(String text,String term,int offset) {
        int end=offset+term.length();
        return !(asciiWord(term.charAt(0))&&offset>0&&asciiWord(text.charAt(offset-1)))
                &&!(asciiWord(term.charAt(term.length()-1))&&end<text.length()&&asciiWord(text.charAt(end)));
    }
    private static boolean asciiWord(char c){return c>='a'&&c<='z'||c>='0'&&c<='9'||c=='_';}
    private static boolean active(Map<String,Object> row) {
        if(Boolean.FALSE.equals(row.get("enabled")))return false;
        Object status=row.get("status");
        return status==null||Set.of("ACTIVE","PUBLISHED","APPROVED").contains(status.toString());
    }
    private static int integer(Object value,int fallback){return value==null?fallback:value instanceof Number n?n.intValue():Integer.parseInt(value.toString());}
    private static long number(Object value){return value instanceof Number n?n.longValue():Long.parseLong(value.toString());}
    private static List<String> terms(Object value) {
        Collection<?> values=value instanceof Collection<?> collection?collection:value instanceof String text?List.of(text):List.of();
        return values.stream().filter(Objects::nonNull).map(Object::toString).map(MatchingEngine::normalize).filter(s->!s.isBlank())
                .distinct().sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder())).toList();
    }
    private static Map<String,Object> map(Object... fields){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<fields.length;i+=2)result.put((String)fields[i],fields[i+1]);return result;}
    @SuppressWarnings("unchecked") private static <T>T freeze(T value){if(value instanceof Map<?,?> source){Map<String,Object> copy=new LinkedHashMap<>();source.forEach((k,v)->copy.put(k.toString(),freeze(v)));return (T)Collections.unmodifiableMap(copy);}if(value instanceof Collection<?> source)return (T)Collections.unmodifiableList(source.stream().map(MatchingEngine::freeze).toList());return value;}
    private static String canonical(Object value){
        if(value==null)return "z";
        if(value instanceof Map<?,?> map){StringBuilder out=new StringBuilder("m{");map.entrySet().stream().sorted(Comparator.comparing(entry->entry.getKey().toString())).forEach(entry->out.append(canonical(entry.getKey().toString())).append(canonical(entry.getValue())));return out.append('}').toString();}
        if(value instanceof Collection<?> list){StringBuilder out=new StringBuilder("l"+list.size()+"[");for(Object item:list)out.append(canonical(item));return out.append(']').toString();}
        if(value instanceof Boolean b)return b?"b1":"b0";
        if(value instanceof Number)return "n"+value+";";
        String text=value.toString();return "s"+text.length()+":"+text;
    }
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}

    private static final class Source {
        final String kind,key,role; final long id,deptId;final int scope,ruleVersion,weight,priority;final boolean enabled;
        final List<String>keywords,negativeKeywords,fixedPhrases;
        Source(String kind,String key,Map<String,Object>row,long deptId,int scope){
            this.kind=kind;this.key=key;this.id=number(row.get("id"));this.deptId=deptId;this.scope=scope;
            ruleVersion=integer(row.get("ruleVersion"),integer(row.get("version"),0)+1);
            weight=integer(row.get("weight"),100);priority=integer(row.get("priority"),0);
            if(weight<0||weight>1000)throw new IllegalArgumentException("Matching weight must be between0and1000");
            enabled=active(row);role=String.valueOf(row.getOrDefault("role","HOST")).toUpperCase(Locale.ROOT);
            fixedPhrases=terms(row.get("fixedPhrases"));
            var configured=terms(row.get("keywords"));
            keywords=configured.isEmpty()&&fixedPhrases.isEmpty()?terms(row.get("name")):configured;
            Set<String> negatives=new LinkedHashSet<>(terms(row.get("negativeKeywords")));negatives.addAll(terms(row.get("negativeWords")));
            negativeKeywords=terms(negatives);
        }
        Map<String,Object> hit(String term,int weight,String type,String outcome,int offset){return map("sourceId",id,"sourceKind",kind,"term",term,"weight",weight,"ruleVersion",ruleVersion,"matchType",type,"outcome",outcome,"offset",offset);}
        Map<String,Object> snapshot(){return map("sourceId",id,"sourceKind",kind,"logicalKey",key,"companyId",scope,"deptId",deptId,"ruleVersion",ruleVersion,"weight",weight,"priority",priority,"enabled",enabled,"role",role,"keywords",keywords,"negativeKeywords",negativeKeywords,"fixedPhrases",fixedPhrases);}
    }
    private static final class Candidate {
        final long deptId;final String deptName;int tier,fixedLength,priority,score;boolean hostEligible;
        final List<Map<String,Object>> hits=new ArrayList<>();
        Candidate(long id,String name){deptId=id;deptName=name;}
        boolean sameRank(Candidate other){return tier==other.tier&&fixedLength==other.fixedLength&&priority==other.priority&&score==other.score;}
    }
}
