package com.dts.progress.adminreport;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** BE06-09: owner-scoped, no-store operational evidence. No cross-service success is inferred. */
@RestController
public class AdminOperationalStatusController {
    private final AdminProgressReportController.Report monitor;
    public AdminOperationalStatusController(AdminProgressReportController.Report monitor) { this.monitor=monitor; }

    @GetMapping("/api/v1/progress/admin/operations/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERM_system-status:read')")
    public ResponseEntity<Map<String,Object>> status() {

        Map<String,Object> local=monitor.integration();
        var out=new LinkedHashMap<String,Object>();out.put("sourceService","dts-progress");out.put("asOf",Instant.now());
        out.put("dataStatus","PARTIAL");out.put("localProjectionStatus",local.get("dataStatus"));out.put("consumer",local);
        out.put("practiceProducerStatus",null);out.put("missingSources",List.of("dts-practice"));
        out.put("warnings",List.of("Progress consumer observation is not source delivery confirmation"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(out);
    }
}
