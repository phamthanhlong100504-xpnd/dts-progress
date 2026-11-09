package com.dts.progress.simulationperformance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static com.dts.progress.simulationperformance.SimulationFixtures.*;

@EnabledIfEnvironmentVariable(named="DTS_SIMULATION_INTEGRATION_POSTGRES_TEST",matches="true")
class SimulationOverviewPostgresTest {
    static SimulationTestDatabase db;static JdbcTemplate jdbc;SimulationEventStore store;SimulationProgressService service;long seq;
    @BeforeAll static void schema()throws Exception{db=new SimulationTestDatabase();jdbc=new JdbcTemplate(db.source);}
    @AfterAll static void done(){if(db!=null)db.close();}
    @BeforeEach void setup(){jdbc.execute("TRUNCATE simulation_performance_inbox");jdbc.update("UPDATE simulation_performance_state SET last_sequence=0,backfill_complete=FALSE,capture_started_at=NULL");store=new SimulationEventStore(jdbc,JSON);service=new SimulationProgressService(jdbc,JSON,store);seq=1;accept(begin());accept(catalog(++seq));}
    void accept(ObjectNode e){db.transaction.executeWithoutResult(s->store.accept(e.toString()));}
    void complete(boolean ready){accept(end(++seq));accept(marker(++seq,ready));}
    JsonNode read(String license){return service.read(USER,license,null,null);}
    @Test void zeroIsPracticedButDoesNotBecomePerfectOrABookmark(){accept(practice(++seq,1,0,"B",false));complete(true);var r=read("B");assertEquals(1,r.path("practicedScenarios").intValue());assertEquals(1,r.path("needReviewCount").intValue());assertEquals(0,r.path("perfectScenarios").intValue());assertEquals(0,r.path("bookmarkCount").intValue());}
    @Test void repeatCompletionOnlyImprovesBestNotDistinctCount(){accept(practice(++seq,1,0,"B",false));accept(practice(++seq,1,5,"B",true));complete(true);var r=read("B");assertEquals(1,r.path("practicedScenarios").intValue());assertEquals(1,r.path("perfectScenarios").intValue());assertEquals(0,r.path("needReviewCount").intValue());}
    @Test void marksAndUnmarksSynchronizeIndependentlyOfLearning(){accept(practice(++seq,1,5,"B",true));accept(bookmark(++seq,2,true));accept(bookmark(++seq,3,true));accept(bookmark(++seq,2,false));complete(true);var r=read("B");assertEquals(1,r.path("bookmarkCount").intValue());assertEquals(1,r.path("practicedScenarios").intValue());assertEquals(1,r.path("perfectScenarios").intValue());}
    @Test void sharedBankProgressDoesNotDuplicateAcrossLicenses(){accept(practice(++seq,1,0,"B",false));accept(practice(++seq,2,5,"C1",true));accept(bookmark(++seq,1,true));complete(true);var b=read("B");var c=read("C1");for(String f:List.of("practicedScenarios","needReviewCount","perfectScenarios","bookmarkCount"))assertEquals(b.path(f),c.path(f));assertEquals(2,c.path("practicedScenarios").intValue());}
    @Test void randomAndPresetExamsDoNotAdvanceChapterLearning(){accept(exam(++seq,UUID.randomUUID(),50,"PASSED","B",true));complete(true);var r=read("B");assertEquals(0,r.path("practicedScenarios").intValue());assertEquals(0,r.path("perfectScenarios").intValue());}
    @Test void sourceCatalogOutagePreservesKnownProgressInsteadOfZeroingIt(){accept(practice(++seq,1,5,"B",true));complete(false);var r=read("B");assertFalse(r.path("simulationReady").asBoolean());assertEquals(1,r.path("practicedScenarios").intValue());assertEquals(120,r.path("totalScenarios").intValue());}
    @Test void unknownCatalogIsNotAZeroPercentProgress(){complete(false);var r=service.read(USER,"B","missing-bank",null);assertEquals("CATALOG_PENDING",r.path("dataStatus").asText());assertTrue(r.path("totalScenarios").isNull());assertTrue(r.path("completionPercent").isNull());assertTrue(r.path("bookmarkCount").isNull());}
    @Test void chapterAndOverallCountsUseTheSameDistinctSituations(){for(int i=1;i<=12;i++)accept(practice(++seq,i,i<=5?5:0,"B",i<=5));complete(true);var r=read("B");assertEquals(12,r.path("practicedScenarios").intValue());assertEquals(7,r.path("needReviewCount").intValue());assertEquals(10,r.path("completionPercent").intValue());int total=0,practiced=0,review=0;for(var c:r.path("chapters")){total+=c.path("totalScenarios").intValue();practiced+=c.path("practicedScenarios").intValue();review+=c.path("needReviewCount").intValue();}assertEquals(120,total);assertEquals(12,practiced);assertEquals(7,review);}
    @Test void oldWatermarkKeepsThePreviouslyConfirmedBookmarkState(){accept(bookmark(++seq,1,true));complete(true);long previous=seq;accept(bookmark(++seq,1,false));accept(marker(++seq,true));assertEquals(0,read("B").path("bookmarkCount").intValue());assertEquals(1,service.read(USER,"B",null,previous).path("bookmarkCount").intValue());}
    @Test void anotherActorCannotReadTheOwnedProjection(){accept(practice(++seq,1,5,"B",true));accept(bookmark(++seq,1,true));complete(true);var r=service.read(UUID.randomUUID(),"B",null,null);assertEquals(0,r.path("practicedScenarios").intValue());assertEquals(0,r.path("bookmarkCount").intValue());}
    @Test void progressReadsDoNotWriteSourceOrProjection()throws Exception{accept(practice(++seq,1,0,"B",false));complete(true);long before=jdbc.queryForObject("SELECT COUNT(*)FROM simulation_performance_inbox",Long.class);JsonNode r=read("B");Path dir=Path.of("target/simulation-performance-examples");Files.createDirectories(dir);Files.writeString(dir.resolve("simulation-progress.actual.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(r));read("C1");
        var principal=new com.dts.progress.security.JwtUserDetails(USER,"owned-fixture",List.of(),List.of("progress:read","simulations:read"));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal,null,List.of()));
        try{
            var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new SimulationProgressController(service)).setControllerAdvice(new SimulationProgressAdvice())
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(JSON.copy().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))).build();
            var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/progress/simulations/learning-overview").param("licenseClass","B"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.practicedScenarios").value(1)).andReturn().getResponse().getContentAsString();
            Files.writeString(dir.resolve("simulation-progress.http.actual.json"),response);
        }finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        assertEquals(before,jdbc.queryForObject("SELECT COUNT(*)FROM simulation_performance_inbox",Long.class));}
}
