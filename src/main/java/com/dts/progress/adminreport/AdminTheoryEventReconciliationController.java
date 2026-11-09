package com.dts.progress.adminreport;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** The theory inbox is committed with its projection, never with a producer's Kafka acknowledgement. */
@RestController
@RequestMapping("/api/v1/progress/admin/integrations")
public class AdminTheoryEventReconciliationController {
    private final Monitor monitor;
    public AdminTheoryEventReconciliationController(Monitor monitor){this.monitor=monitor;}
    @GetMapping("/theory-events/{eventId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERM_integrations:read')")
    public ResponseEntity<Map<String,Object>> event(@PathVariable UUID eventId){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(monitor.event(eventId));
    }
    @Service
    @Transactional(readOnly=true)
    public static class Monitor {
        private final JdbcTemplate jdbc;
        public Monitor(JdbcTemplate jdbc){this.jdbc=jdbc;}
        public Map<String,Object> event(UUID eventId){
            if(eventId==null)throw new IllegalArgumentException("eventId required");
            List<Map<String,Object>> rows=jdbc.queryForList("""
                SELECT event_id,event_sequence,received_at,
                  EXISTS (SELECT 1 FROM user_question_progress p
                          WHERE p.user_id=i.user_id AND p.license_class=i.license_class
                          AND p.bank_version=i.bank_version AND p.question_id=i.question_id)
                          AS projection_row_present
                FROM practice_learning_event_inbox i WHERE event_id=?
                """,eventId);
            if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Event has not been observed in Progress");
            Map<String,Object> row=rows.getFirst();
            var out=new LinkedHashMap<String,Object>();out.put("sourceService","dts-progress");
            out.put("asOf",Instant.now());out.put("dataStatus","READY");
            out.put("eventId",eventId);out.put("eventSequence",row.get("event_sequence"));
            out.put("receivedAt",row.get("received_at"));out.put("consumerReceived",true);
            out.put("projectionWriteCommittedWithInbox",true);
            out.put("projectionRowPresent",row.get("projection_row_present"));
            out.put("projectionApplied",Boolean.TRUE.equals(row.get("projection_row_present")));
            out.put("producerAcknowledged",null);return out;
        }
    }
}
