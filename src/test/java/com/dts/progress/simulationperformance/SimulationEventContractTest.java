package com.dts.progress.simulationperformance;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.dts.progress.simulationperformance.SimulationFixtures.*;

class SimulationEventContractTest {
    void valid(ObjectNode e){assertNotNull(SimulationEventStore.parse(e.toString(),JSON));}
    void invalid(ObjectNode e){assertThrows(IllegalArgumentException.class,()->SimulationEventStore.parse(e.toString(),JSON));}
    @Test void parsesTheSourceOwnedExamAndSnapshotN(){var e=exam(2,UUID.randomUUID(),40,"PASSED","C1",true);assertEquals(35,SimulationEventStore.parse(e.toString(),JSON).data().path("nSnapshot").intValue());}
    @Test void aSourceFailureIsNotRecomputedFromHighNormalizedScore(){var e=exam(2,UUID.randomUUID(),40,"FAILED","B",true);((ObjectNode)e.get("data")).put("passingScore",45);valid(e);}
    @Test void sourcePassedIsNotRecomputedWithTodayThreshold(){var e=exam(2,UUID.randomUUID(),34,"PASSED","B",true);((ObjectNode)e.get("data")).put("passingScore",30);valid(e);}
    @Test void nullableDurationStaysNullable(){var e=exam(2,UUID.randomUUID(),0,"FAILED","B",true);valid(e);assertTrue(e.path("data").path("durationSeconds").isNull());}
    @Test void nIsRequiredBeforeAwarding(){var e=exam(2,UUID.randomUUID(),40,"PASSED","B",true);((ObjectNode)e.get("data")).putNull("nSnapshot");invalid(e);}
    @Test void oldUncreditedFactsMayHaveNoHistoricalN(){var e=exam(2,UUID.randomUUID(),40,"PASSED","B",false);((ObjectNode)e.get("data")).putNull("nSnapshot");valid(e);}
    @Test void zeroScorePracticeIsACompletedPracticeFact(){valid(practice(2,1,0,"B",false));}
    @Test void aZeroCannotClaimFirstPerfectCredit(){invalid(practice(2,1,0,"B",true));}
    @Test void currentBookmarkFalseIsATombstone(){var e=bookmark(2,1,false);valid(e);assertFalse(SimulationEventStore.parse(e.toString(),JSON).data().path("marked").booleanValue());}
    @Test void bookMarkMustBeBoolean(){var e=bookmark(2,1,false);((ObjectNode)e.get("data")).put("marked","false");invalid(e);}
    @Test void confirmsExactCanonicalCatalog(){valid(catalog(2));}
    @Test void rejectsMismatchingChapterDenominators(){var e=catalog(2);((ObjectNode)e.path("data").path("chapters").get(0)).put("totalScenarios",28);invalid(e);}
    @Test void rejectsDuplicateScenarioIds(){var e=catalog(2);((ObjectNode)e.path("data").path("scenarios").get(1)).put("scenarioId",scenario(1).toString());invalid(e);}
    @Test void markerMustMatchItsOwnSourceSequence(){var e=marker(4,true);((ObjectNode)e.get("data")).put("throughSequence",4);invalid(e);}
    @Test void rejectsUnsupportedLicenseWithoutChangingLegacyPolicy(){invalid(exam(2,UUID.randomUUID(),40,"PASSED","A1",true));assertFalse(SimulationEventStore.supported("B1"));}
    @Test void cannotAwardBeforeCapture(){var e=exam(2,UUID.randomUUID(),40,"PASSED","B",true);((ObjectNode)e.get("data")).put("captureStartedAt",NOW.plusSeconds(1).toString());invalid(e);}
    @Test void rejectsInvalidSourceOrVersion(){var e=begin();e.put("source","browser");invalid(e);e=begin();e.put("eventVersion",2);invalid(e);}
    @Test void rejectsNoncanonicalUuidAndInvalidNumbers(){var e=exam(2,UUID.randomUUID(),40,"PASSED","B",true);e.put("eventId","1-1-1-1-1");invalid(e);e=begin();e.put("eventSequence",0);invalid(e);}
    @Test void mismatchedModeAndActivityCannotBecomeAResult(){var e=exam(2,UUID.randomUUID(),40,"PASSED","B",true);((ObjectNode)e.get("data")).put("mode","PRESET");invalid(e);}
    @Test void schemaRejectsFabricatedFiftyPlusScores(){invalid(exam(2,UUID.randomUUID(),51,"PASSED","B",true));}
    @Test void markerTimestampsAreAlwaysIsoStringsIndependentOfMapperDefaults(){
        var m=new SimulationEventStore.Marker(1,NOW,NOW,true,false,"bank-v1","READY",1,1);
        var value=JSON.valueToTree(m);assertTrue(value.path("asOf").isTextual());assertEquals(NOW.toString(),value.path("asOf").asText());assertTrue(value.path("receivedAt").isTextual());
    }
}
