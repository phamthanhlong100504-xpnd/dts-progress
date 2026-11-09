package com.dts.progress.simulationperformance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Same wire parser and gap-aware, immutable inbox in Result and Progress. */
@Service
public class SimulationEventStore {
    private static final Set<String> TYPES=Set.of("SIMULATION_SYNC_BEGIN","SIMULATION_SYNC_END","SIMULATION_SYNC_MARKER",
        "SIMULATION_POLICY_CHANGED","SIMULATION_CATALOG_PUBLISHED","SIMULATION_EXAM_COMPLETED",
        "SIMULATION_PRACTICE_COMPLETED","SIMULATION_BOOKMARK_CHANGED");
    private static final Set<String> LICENSES=Set.of("B","C1","C","D1","D2","D","BE","C1E","CE","D1E","D2E","DE");
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;
    public SimulationEventStore(JdbcTemplate jdbc,ObjectMapper mapper) {this.jdbc=jdbc;this.mapper=mapper;}
    public record Event(UUID id,long sequence,String type,Instant occurredAt,JsonNode data) {}
    public static boolean supported(String license) {return license!=null && LICENSES.contains(license);}
    public static String requireLicense(String license) {check(supported(license),"Simulation is not supported for this licenseClass");return license;}
    public static Event parse(String body,ObjectMapper mapper) {
        try {
            check(body!=null && body.length()<=2_000_000,"Invalid simulation event size");
            JsonNode root=mapper.readTree(body);check(root!=null && root.isObject(),"Invalid simulation event");
            UUID id=uuid(root,"eventId",false);long seq=number(root,"eventSequence",1,Long.MAX_VALUE);
            check(number(root,"eventVersion",1,1)==1 && "dts-practice".equals(text(root,"source",40,false)),"Invalid simulation source/version");
            String type=text(root,"eventType",64,false);check(TYPES.contains(type),"Unsupported simulation event type");
            Instant at=instant(root,"occurredAt");JsonNode d=root.get("data");check(d!=null && d.isObject(),"Event data is required");
            instant(d,"captureStartedAt");
            switch(type) {
                case "SIMULATION_SYNC_BEGIN" -> check(seq==1 && d.path("backfill").asBoolean(false),"Invalid capture begin");
                case "SIMULATION_SYNC_END" -> check(d.path("backfillComplete").asBoolean(false),"Invalid backfill completion");
                case "SIMULATION_SYNC_MARKER" -> {
                    bool(d,"bankReady");text(d,"currentBankVersion",96,true);
                    check(number(d,"throughSequence",0,Long.MAX_VALUE)==seq-1,"Invalid source marker");
                }
                case "SIMULATION_POLICY_CHANGED" -> {check(d.path("questionCounts").isObject(),"Invalid source policy");}
                case "SIMULATION_CATALOG_PUBLISHED" -> validateCatalog(d);
                case "SIMULATION_BOOKMARK_CHANGED" -> {
                    uuid(d,"userId",false);uuid(d,"scenarioId",false);text(d,"bankVersion",96,false);bool(d,"marked");
                }
                case "SIMULATION_PRACTICE_COMPLETED" -> {
                    commonFact(d,at);uuid(d,"scenarioId",false);number(d,"scenarioNumber",1,120);number(d,"chapterId",1,6);
                    number(d,"score",0,5);check(number(d,"maxScore",5,5)==5,"Invalid practice maximum");
                    check("SIMULATION_ATTEMPT".equals(text(d,"sourceType",40,false)),"Invalid practice source");
                    check("DTS_SIMULATION_120_V1".equals(text(d,"achievementScope",64,false)),"Invalid achievement scope");
                    check("SIMULATION_EXP_V1".equals(text(d,"expPolicyVersion",64,false)),"Unsupported EXP policy");
                    boolean first=bool(d,"firstPerfectEligible");
                    check(!first || (d.path("score").intValue()==5 && !at.isBefore(instant(d,"captureStartedAt"))),"Invalid first-perfect eligibility");
                }
                case "SIMULATION_EXAM_COMPLETED" -> {
                    commonFact(d,at);number(d,"score",0,50);number(d,"maxScore",50,50);
                    check("SIMULATION_SESSION".equals(text(d,"sourceType",40,false)),"Invalid exam source");
                    String mode=text(d,"mode",12,false);check(Set.of("RANDOM","PRESET").contains(mode),"Invalid exam mode");
                    check(("SIM_"+mode+"_EXAM").equals(text(d,"activityType",32,false)),"Invalid activity type");
                    check(Set.of("PASSED","FAILED").contains(text(d,"result",8,false)),"Invalid source outcome");
                    text(d,"displayTitle",200,false);text(d,"scoringVersion",128,false);
                    check(!instant(d,"startedAt").isAfter(at),"Invalid source chronology");
                    JsonNode duration=d.get("durationSeconds");check(duration==null || duration.isNull() || (duration.isIntegralNumber() && duration.canConvertToLong() && duration.longValue()>=0),"Invalid source duration");
                    check("SIMULATION_EXP_V1".equals(text(d,"expPolicyVersion",64,false)),"Unsupported EXP policy");number(d,"expPolicyRevision",1,Long.MAX_VALUE);
                    boolean eligible=bool(d,"expEligible");JsonNode n=d.get("nSnapshot");
                    check(n==null || n.isNull() || (n.isIntegralNumber() && n.canConvertToInt() && n.intValue()>0 && n.intValue()<=1000),"Invalid N snapshot");
                    check(!eligible || (n!=null && n.isIntegralNumber() && !at.isBefore(instant(d,"captureStartedAt"))),"EXP must have a source-owned N snapshot and cutover");
                    UUID preset=uuid(d,"presetSetId",true),version=uuid(d,"presetVersionId",true);
                    check(mode.equals("PRESET") ? preset!=null && version!=null : preset==null && version==null,"Invalid preset reference");
                }
                default -> throw new IllegalArgumentException("Unsupported event");
            }
            return new Event(id,seq,type,at,d);
        } catch(IllegalArgumentException e) {throw e;}
        catch(Exception e) {throw new IllegalArgumentException("Malformed simulation event");}
    }
    private static void commonFact(JsonNode d,Instant at) {
        uuid(d,"userId",false);uuid(d,"sourceId",false);requireLicense(text(d,"licenseClass",16,false));
        text(d,"bankVersion",96,false);check(instant(d,"completedAt").equals(at),"Source completedAt must be authoritative");
    }
    private static void validateCatalog(JsonNode d) {
        text(d,"bankVersion",96,false);String manifest=text(d,"manifestSha256",64,false);check(manifest.matches("[a-f0-9]{64}"),"Invalid catalog manifest");
        check(d.path("chapters").isArray() && d.path("chapters").size()==6 && d.path("scenarios").isArray() && d.path("scenarios").size()==120,"Invalid canonical catalog size");
        var chapters=new HashMap<Integer,Integer>();
        for(JsonNode c:d.path("chapters")) {int n=(int)number(c,"chapterId",1,6);text(c,"name",200,false);check(chapters.put(n,(int)number(c,"totalScenarios",1,120))==null,"Duplicate chapter");}
        var counts=new HashMap<Integer,Integer>();var ids=new HashSet<UUID>();var numbers=new HashSet<Long>();
        for(JsonNode s:d.path("scenarios")) {check(ids.add(uuid(s,"scenarioId",false)) && numbers.add(number(s,"scenarioNumber",1,120)),"Duplicate scenario");counts.merge((int)number(s,"chapterId",1,6),1,Integer::sum);}
        check(chapters.equals(counts),"Inconsistent chapter counts");
    }
    @Transactional
    public void accept(String body) {
        Event event=parse(body,mapper);JsonNode d=event.data();
        jdbc.queryForList("SELECT last_sequence FROM simulation_performance_state WHERE singleton FOR UPDATE");
        UUID user=uuid(d,"userId",true),source=uuid(d,"sourceId",true);String license=text(d,"licenseClass",16,true),bank=text(d,"bankVersion",96,true);
        jdbc.update("INSERT INTO simulation_performance_inbox(event_sequence,event_id,event_type,occurred_at,data,user_id,license_class,source_id,bank_version) VALUES(?,?,?,?,?::jsonb,?,?,?,?) ON CONFLICT DO NOTHING",
                event.sequence(),event.id(),event.type(),Timestamp.from(event.occurredAt()),d.toString(),user,license,source,bank);
        Boolean same=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM simulation_performance_inbox WHERE event_sequence=? AND event_id=? AND event_type=? AND occurred_at=? AND data=?::jsonb)",Boolean.class,
                event.sequence(),event.id(),event.type(),Timestamp.from(event.occurredAt()),d.toString());
        check(Boolean.TRUE.equals(same),"An event ID/sequence cannot replace different source evidence");
        for(int count=0;count<1000;count++) {
            long last=jdbc.queryForObject("SELECT last_sequence FROM simulation_performance_state WHERE singleton",Long.class);
            var next=jdbc.query("SELECT event_id,event_type,occurred_at,data::text FROM simulation_performance_inbox WHERE event_sequence=? AND NOT applied",
                    (r,n)->new Event(r.getObject(1,UUID.class),last+1,r.getString(2),r.getTimestamp(3).toInstant(),tree(r.getString(4))),last+1);
            if(next.isEmpty()) break;
            Event e=next.getFirst();Instant captured=instant(e.data(),"captureStartedAt");
            if(e.sequence()==1) {
                check(e.type().equals("SIMULATION_SYNC_BEGIN"),"Capture must begin at sequence1");
                jdbc.update("UPDATE simulation_performance_state SET capture_started_at=? WHERE singleton",Timestamp.from(captured));
            } else {
                Instant expected=jdbc.queryForObject("SELECT capture_started_at FROM simulation_performance_state WHERE singleton",Timestamp.class).toInstant();
                check(expected.equals(captured),"A different source epoch requires explicit reconciliation");
            }
            if(Set.of("SIMULATION_EXAM_COMPLETED","SIMULATION_PRACTICE_COMPLETED").contains(e.type())) {
                UUID id=uuid(e.data(),"sourceId",false);
                var older=jdbc.queryForList("SELECT data::text FROM simulation_performance_inbox WHERE applied AND event_type=? AND source_id=? LIMIT 1",String.class,e.type(),id);
                check(older.isEmpty() || tree(older.getFirst()).equals(e.data()),"A source result cannot be replaced by a second event");
            }
            if(e.type().equals("SIMULATION_CATALOG_PUBLISHED")) {
                var older=jdbc.queryForList("SELECT data::text FROM simulation_performance_inbox WHERE applied AND event_type=? AND bank_version=? LIMIT 1",String.class,e.type(),text(e.data(),"bankVersion",96,false));
                check(older.isEmpty() || tree(older.getFirst()).equals(e.data()),"Published bank version is immutable");
            }
            if(e.type().equals("SIMULATION_SYNC_END")) jdbc.update("UPDATE simulation_performance_state SET backfill_complete=TRUE WHERE singleton");
            jdbc.update("UPDATE simulation_performance_inbox SET applied=TRUE WHERE event_sequence=?",e.sequence());
            jdbc.update("UPDATE simulation_performance_state SET last_sequence=? WHERE singleton",e.sequence());
        }
    }
    public record Marker(long sequence,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant asOf,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant receivedAt,boolean complete,boolean bankReady,String bankVersion,String status,long contiguousSequence,long pendingThrough) {}
    public Marker marker(Long requested) {
        var state=jdbc.queryForMap("SELECT last_sequence,backfill_complete FROM simulation_performance_state WHERE singleton");
        long through=((Number)state.get("last_sequence")).longValue();boolean complete=Boolean.TRUE.equals(state.get("backfill_complete"));
        long pending=jdbc.queryForObject("SELECT COALESCE(MAX(event_sequence),0) FROM simulation_performance_inbox",Long.class);
        var markers=jdbc.query("SELECT event_sequence,occurred_at,received_at,data::text FROM simulation_performance_inbox WHERE applied AND event_type='SIMULATION_SYNC_MARKER'"+(requested==null?" ORDER BY event_sequence DESC LIMIT 1":" AND event_sequence=?"),
                (r,n)->new Marker(r.getLong(1),r.getTimestamp(2).toInstant(),r.getTimestamp(3).toInstant(),complete,tree(r.getString(4)).path("bankReady").asBoolean(false),text(tree(r.getString(4)),"currentBankVersion",96,true),"READY",through,pending),requested==null?new Object[]{}:new Object[]{requested});
        check(requested==null || !markers.isEmpty(),"Requested watermark is not a confirmed source marker");
        if(markers.isEmpty()) return new Marker(0,null,null,false,false,null,"SYNCING",through,pending);
        var m=markers.getFirst();String status=!complete?"BACKFILLING":pending>through || through>m.sequence()?"SYNCING":m.asOf().isBefore(Instant.now().minusSeconds(120))?"STALE":"READY";
        return new Marker(m.sequence(),m.asOf(),m.receivedAt(),complete,m.bankReady(),m.bankVersion(),status,through,pending);
    }
    public JsonNode catalog(String bank,long watermark) {
        var values=jdbc.queryForList("SELECT data::text FROM simulation_performance_inbox WHERE applied AND event_type='SIMULATION_CATALOG_PUBLISHED' AND bank_version=? AND event_sequence<=? ORDER BY event_sequence DESC LIMIT 1",String.class,bank,watermark);
        return values.isEmpty()?null:tree(values.getFirst());
    }
    private JsonNode tree(String body) {try{return mapper.readTree(body);}catch(Exception e){throw new IllegalStateException("Invalid stored simulation evidence");}}
    static void check(boolean ok,String message) {if(!ok)throw new IllegalArgumentException(message);}
    static String text(JsonNode n,String key,int max,boolean nullable) {
        JsonNode v=n.get(key);if(nullable && (v==null || v.isNull()))return null;
        check(v!=null && v.isTextual() && !v.textValue().isBlank() && v.textValue().length()<=max,"Invalid field: "+key);return v.textValue();
    }
    static UUID uuid(JsonNode n,String key,boolean nullable) {String s=text(n,key,36,nullable);if(s==null)return null;UUID id=UUID.fromString(s);check(id.toString().equals(s),"Noncanonical UUID: "+key);return id;}
    static long number(JsonNode n,String key,long low,long high) {JsonNode v=n.get(key);check(v!=null && v.isIntegralNumber() && v.canConvertToLong() && v.longValue()>=low && v.longValue()<=high,"Invalid integer: "+key);return v.longValue();}
    static boolean bool(JsonNode n,String key) {JsonNode v=n.get(key);check(v!=null && v.isBoolean(),"Invalid boolean: "+key);return v.booleanValue();}
    static Instant instant(JsonNode n,String key) {return OffsetDateTime.parse(text(n,key,64,false)).toInstant();}
}
