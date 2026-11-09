package com.dts.progress.adminreport;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProgressEventReadServiceTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final ProgressEventReadService service=new ProgressEventReadService(jdbc);

    @Test void malformedScopeCannotQueryDatabase(){
        assertThrows(IllegalArgumentException.class,()->service.list(-1,20,"ALL",null));
        assertThrows(IllegalArgumentException.class,()->service.list(0,101,"ALL",null));
        assertThrows(IllegalArgumentException.class,()->service.list(0,20,"PUBLISHED",null));
        assertThrows(IllegalArgumentException.class,()->service.list(0,20,"ALL","not-a-uuid"));
        verifyNoInteractions(jdbc);
    }
    @Test void emptyInboxIsRealZeroNotSyntheticFailure(){
        when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenReturn(0L);
        when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of());
        Map<String,Object> result=service.list(0,20,"ALL",null);
        assertEquals("dts-progress",result.get("sourceService"));
        assertEquals("READY",result.get("dataStatus"));
        assertEquals(0L,result.get("totalElements"));
        assertEquals(List.of(),result.get("items"));
        assertNull(result.get("producerAcknowledged"));
    }
    @Test void resultQueryContainsNoLearnerPayloadColumns(){
        when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenReturn(1L);
        when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of(
            "event_id",UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "event_type","SIMULATION_EXAM_COMPLETED","applied",false)));
        service.list(2,10,"PENDING",null);
        var query=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(query.capture(),any(Object[].class));
        String sql=query.getValue().toLowerCase();
        assertTrue(sql.contains("applied=false"));
        assertTrue(sql.contains("limit ? offset ?"));
        assertFalse(sql.contains("select *"));
        assertFalse(sql.contains("user_id"));
        assertFalse(sql.contains("data jsonb"));
    }
}
