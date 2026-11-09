package com.dts.progress.simulationperformance;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static com.dts.progress.simulationperformance.SimulationFixtures.*;

@EnabledIfEnvironmentVariable(named="DTS_SIMULATION_INTEGRATION_POSTGRES_TEST",matches="true")
class SimulationProjectionPostgresTest {
    static SimulationTestDatabase db;static JdbcTemplate jdbc;SimulationEventStore store;
    @BeforeAll static void schema() throws Exception{db=new SimulationTestDatabase();jdbc=new JdbcTemplate(db.source);}
    @AfterAll static void done(){if(db!=null)db.close();}
    @BeforeEach void setup(){jdbc.execute("TRUNCATE simulation_performance_inbox");jdbc.update("UPDATE simulation_performance_state SET last_sequence=0,backfill_complete=FALSE,capture_started_at=NULL");store=new SimulationEventStore(jdbc,JSON);}
    void accept(com.fasterxml.jackson.databind.node.ObjectNode e){db.transaction.executeWithoutResult(s->store.accept(e.toString()));}
    long size(){return jdbc.queryForObject("SELECT COUNT(*)FROM simulation_performance_inbox",Long.class);}
    @Test void exactReplayNeverAddsAnotherFact(){accept(begin());var event=practice(2,1,0,"B",false);accept(event);accept(event);assertEquals(2,size());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM simulation_performance_practice_facts",Integer.class));}
    @Test void lateLowerSequenceDrainsTheGapWithoutLosingAZeroScore(){accept(begin());accept(end(3));assertEquals(1,jdbc.queryForObject("SELECT last_sequence FROM simulation_performance_state",Integer.class));accept(practice(2,1,0,"B",false));assertEquals(3,jdbc.queryForObject("SELECT last_sequence FROM simulation_performance_state",Integer.class));assertTrue(store.marker(null).complete()==false);accept(marker(4,true));assertTrue(store.marker(null).complete());}
    @Test void sameIdOrSequenceCannotReplaceADifferentResult(){accept(begin());var event=exam(2,UUID.randomUUID(),40,"PASSED","B",true);accept(event);var changed=event.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)changed.get("data")).put("score",50);assertThrows(IllegalArgumentException.class,()->accept(changed));assertEquals(40,jdbc.queryForObject("SELECT source_score FROM simulation_performance_exam_facts",Integer.class));}
    @Test void aSecondEventIdForIdenticalSourceIsDeduplicated(){accept(begin());var e=exam(2,UUID.randomUUID(),40,"PASSED","B",true);accept(e);var alias=e.deepCopy();alias.put("eventSequence",3);alias.put("eventId",UUID.randomUUID().toString());accept(alias);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*)FROM simulation_performance_exam_facts",Integer.class));}
    @Test void aSourceIdCannotCarryDifferentOutcomeOnReplay(){accept(begin());var e=exam(2,UUID.randomUUID(),40,"PASSED","B",true);accept(e);var alias=e.deepCopy();alias.put("eventSequence",3);alias.put("eventId",UUID.randomUUID().toString());((com.fasterxml.jackson.databind.node.ObjectNode)alias.get("data")).put("result","FAILED");assertThrows(IllegalArgumentException.class,()->accept(alias));assertEquals(2,size());}
    @Test void tombstonesRetainTheLatestStateAfterOutOfOrderDelivery(){accept(begin());accept(bookmark(3,1,false));accept(bookmark(2,1,true));Boolean marked=jdbc.queryForObject("SELECT(data->>'marked')::boolean FROM simulation_performance_inbox WHERE event_type='SIMULATION_BOOKMARK_CHANGED' AND applied ORDER BY event_sequence DESC LIMIT 1",Boolean.class);assertEquals(Boolean.FALSE,marked);}
    @Test void rollbackOfTheConsumerDoesNotAcknowledgeOrAdvanceTheInbox(){accept(begin());assertThrows(RuntimeException.class,()->db.transaction.executeWithoutResult(s->{store.accept(practice(2,1,5,"B",true).toString());throw new RuntimeException("owned rollback fixture");}));assertEquals(1,size());assertEquals(1,jdbc.queryForObject("SELECT last_sequence FROM simulation_performance_state",Integer.class));}
    @Test void watermarkReturnsOnlyAConfirmedPrefix(){accept(begin());accept(end(2));accept(marker(3,false));accept(practice(4,1,5,"B",true));assertEquals(3,store.marker(null).sequence());assertFalse(store.marker(null).bankReady());assertThrows(IllegalArgumentException.class,()->store.marker(4L));}
    @Test void publishedCatalogCannotChangeUnderTheSameBankVersion(){accept(begin());accept(catalog(2));var changed=catalog(3);changed.with("data").put("manifestSha256","b".repeat(64));assertThrows(IllegalArgumentException.class,()->accept(changed));assertEquals(2,size());}
    @Test void concurrentExactDeliveryIsSerializableAndIdempotent() throws Exception{
        accept(begin());var e=practice(2,1,5,"B",true);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->accept(e));var b=pool.submit(()->accept(e));a.get();b.get();}finally{pool.shutdownNow();}
        assertEquals(2,size());assertEquals(2,jdbc.queryForObject("SELECT last_sequence FROM simulation_performance_state",Integer.class));
    }
}
