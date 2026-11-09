package com.dts.progress.simulationperformance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

final class SimulationFixtures {
    static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    static final Instant NOW=Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.MICROS);
    static final Instant CAPTURE=NOW.minusSeconds(86400);
    static final UUID USER=UUID.fromString("10000000-0000-4000-8000-000000000001");
    static UUID scenario(int number){return new UUID(0,number);}
    static ObjectNode data(){var d=JSON.createObjectNode();d.put("captureStartedAt",CAPTURE.toString());return d;}
    static ObjectNode event(long sequence,String type,Instant at,ObjectNode data){
        var e=JSON.createObjectNode();e.put("eventId",new UUID(1,sequence).toString());e.put("eventType",type);e.put("eventVersion",1);
        e.put("eventSequence",sequence);e.put("source","dts-practice");e.put("occurredAt",at.toString());e.set("data",data);return e;
    }
    static ObjectNode begin(){var d=data();d.put("backfill",true);d.put("achievementScope","DTS_SIMULATION_120_V1");return event(1,"SIMULATION_SYNC_BEGIN",CAPTURE,d);}
    static ObjectNode end(long sequence){var d=data();d.put("backfillComplete",true);return event(sequence,"SIMULATION_SYNC_END",NOW.minusSeconds(2),d);}
    static ObjectNode marker(long sequence,boolean ready){var d=data();d.put("bankReady",ready);d.put("currentBankVersion","bank-v1");d.put("throughSequence",sequence-1);return event(sequence,"SIMULATION_SYNC_MARKER",NOW,d);}
    static ObjectNode catalog(long sequence){
        var d=data();d.put("bankVersion","bank-v1");d.put("manifestSha256","a".repeat(64));
        d.put("achievementScope","DTS_SIMULATION_120_V1");d.put("learningScope","USER_BANK_VERSION_SHARED_LICENSES");
        var chapters=d.putArray("chapters");var scenarios=d.putArray("scenarios");int ordinal=0;int[] sizes={29,14,20,10,17,30};
        for(int i=0;i<6;i++){var c=chapters.addObject();c.put("chapterId",i+1);c.put("name","Chapter"+(i+1));c.put("totalScenarios",sizes[i]);
            for(int j=0;j<sizes[i];j++){ordinal++;var s=scenarios.addObject();s.put("scenarioId",scenario(ordinal).toString());s.put("scenarioNumber",ordinal);s.put("chapterId",i+1);}}
        return event(sequence,"SIMULATION_CATALOG_PUBLISHED",NOW.minusSeconds(3),d);
    }
    static ObjectNode exam(long sequence,UUID source,int score,String outcome,String license,boolean eligible){
        var d=data();d.put("userId",USER.toString());d.put("licenseClass",license);d.put("sourceType","SIMULATION_SESSION");d.put("sourceId",source.toString());
        d.put("activityType","SIM_RANDOM_EXAM");d.put("mode","RANDOM");d.put("bankVersion","bank-v1");d.put("scoringVersion","scoring-v1");
        d.put("manifestSha256","a".repeat(64));d.put("score",score);d.put("maxScore",50);d.put("result",outcome);
        Instant completed=NOW.minusSeconds(20).plusMillis(sequence);d.put("completedAt",completed.toString());d.put("startedAt",completed.minusSeconds(300).toString());
        d.putNull("durationSeconds");d.put("scoringPolicyVersion","source-policy");d.put("passingScore",35);d.putNull("presetSetId");d.putNull("presetVersionId");
        d.put("displayTitle","Mock simulation");d.put("expPolicyVersion","SIMULATION_EXP_V1");d.put("expPolicyRevision",1);d.put("nSnapshot",license.equals("C1")?35:30);
        d.put("expEligible",eligible);if(eligible)d.putNull("expExclusionReason");else d.put("expExclusionReason","HISTORICAL_BACKFILL");
        return event(sequence,"SIMULATION_EXAM_COMPLETED",completed,d);
    }
    static ObjectNode practice(long sequence,int number,int score,String license,boolean first){
        var d=data();d.put("userId",USER.toString());d.put("licenseClass",license);d.put("sourceType","SIMULATION_ATTEMPT");d.put("sourceId",new UUID(2,sequence).toString());
        d.put("bankVersion","bank-v1");d.put("scoringVersion","scoring-v1");d.put("scenarioId",scenario(number).toString());d.put("scenarioNumber",number);d.put("chapterId",1);
        d.put("score",score);d.put("maxScore",5);Instant at=NOW.minusSeconds(30).plusMillis(sequence);d.put("completedAt",at.toString());
        d.put("achievementScope","DTS_SIMULATION_120_V1");d.put("expPolicyVersion","SIMULATION_EXP_V1");d.put("firstPerfectEligible",first);return event(sequence,"SIMULATION_PRACTICE_COMPLETED",at,d);
    }
    static ObjectNode bookmark(long sequence,int number,boolean marked){var d=data();d.put("userId",USER.toString());d.put("bankVersion","bank-v1");d.put("scenarioId",scenario(number).toString());d.put("marked",marked);return event(sequence,"SIMULATION_BOOKMARK_CHANGED",NOW.minusSeconds(10).plusMillis(sequence),d);}
}
