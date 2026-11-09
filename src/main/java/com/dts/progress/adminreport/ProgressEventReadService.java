package com.dts.progress.adminreport;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only, paginated projection evidence. No event payload, user ID or score leaves this service. */
@Service
@Transactional(readOnly = true)
public class ProgressEventReadService {
    private final JdbcTemplate jdbc;
    public ProgressEventReadService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String,Object> list(int page,int size,String status,String keyword) {
        if (page < 0 || page > 1000 || size < 1 || size > 100)
            throw new IllegalArgumentException("Invalid event page or size");
        String state = status == null || status.isBlank() ? "ALL" : status;
        if (!List.of("ALL","APPLIED","PENDING").contains(state))
            throw new IllegalArgumentException("Unknown event status");
        UUID eventId = null;
        if(keyword != null && !keyword.isBlank()) {
            if(keyword.length() > 36) throw new IllegalArgumentException("Invalid event identifier");
            try { eventId=UUID.fromString(keyword); }
            catch(IllegalArgumentException e) { throw new IllegalArgumentException("Event keyword must be UUID",e); }
        }
        StringBuilder predicate=new StringBuilder(" WHERE 1=1");
        var args=new java.util.ArrayList<Object>();
        if("APPLIED".equals(state)){predicate.append(" AND applied=TRUE");}
        if("PENDING".equals(state)){predicate.append(" AND applied=FALSE");}
        if(eventId!=null){predicate.append(" AND event_id=?");args.add(eventId);}
        Long found=jdbc.queryForObject("SELECT COUNT(*) FROM simulation_performance_inbox"+predicate,Long.class,args.toArray());
        var pageArgs=new java.util.ArrayList<Object>(args);pageArgs.add(size);pageArgs.add((long)page*size);
        List<Map<String,Object>> items=jdbc.queryForList("""
            SELECT event_id,event_type,event_sequence,occurred_at,received_at,applied
            FROM simulation_performance_inbox
            """+predicate+" ORDER BY received_at DESC,event_sequence DESC LIMIT ? OFFSET ?",pageArgs.toArray());
        var out=new LinkedHashMap<String,Object>();out.put("sourceService","dts-progress");
        out.put("dataStatus","READY");out.put("asOf",Instant.now());out.put("page",page);
        out.put("size",size);out.put("totalElements",found==null?0:found);
        out.put("items",items);out.put("producerAcknowledged",null);
        out.put("deliveryStatus","CONSUMER_OBSERVED");
        out.put("warning","Inbox observation does not prove broker acknowledgement or other consumers applied the event");
        return out;
    }
}
