package com.dts.progress.adminreport;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminTheoryEventReconciliationTest {
    private final UUID id=new UUID(0,7);
    @Test void monitoringRequiresAdminReportGrant() throws Exception {
        var method=AdminTheoryEventReconciliationController.class.getMethod("event",UUID.class);
        PreAuthorize grant=method.getAnnotation(PreAuthorize.class);
        assertNotNull(grant);assertTrue(grant.value().contains("ROLE_ADMIN"));
        assertTrue(grant.value().contains("PERM_integrations:read"));
    }
    @Test void consumerEvidenceIsNotInventedWhenInboxIsAbsent(){
        JdbcTemplate db=mock(JdbcTemplate.class);
        when(db.queryForList(anyString(),eq(id))).thenReturn(List.of());
        var model=new AdminTheoryEventReconciliationController.Monitor(db);
        assertThrows(ResponseStatusException.class,()->model.event(id));
        verify(db).queryForList(anyString(),eq(id));
    }
    @Test void projectionEvidenceIsConditionalButNeverProvesProducerAck(){
        JdbcTemplate db=mock(JdbcTemplate.class);
        when(db.queryForList(anyString(),eq(id))).thenReturn(List.of(Map.of("event_sequence",5L,"projection_row_present",true)));
        var model=new AdminTheoryEventReconciliationController.Monitor(db);
        var evidence=model.event(id);
        assertEquals(true,evidence.get("projectionApplied"));
        assertNull(evidence.get("producerAcknowledged"));
        assertEquals("no-store",new AdminTheoryEventReconciliationController(model).event(id).getHeaders().getFirst("Cache-Control"));
    }
}
