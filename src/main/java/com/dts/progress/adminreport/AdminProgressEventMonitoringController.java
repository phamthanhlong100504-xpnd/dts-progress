package com.dts.progress.adminreport;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Owner-only, no-store event list for Admin operations. */
@RestController
@RequestMapping("/api/v1/progress/admin/integrations")
public class AdminProgressEventMonitoringController {
    private final ProgressEventReadService events;
    public AdminProgressEventMonitoringController(ProgressEventReadService events){this.events=events;}
    @GetMapping("/events")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERM_integrations:read')")
    public ResponseEntity<Map<String,Object>> list(
        @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size,
        @RequestParam(required=false) String status,
        @RequestParam(required=false) String keyword){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(events.list(page,size,status,keyword));
    }
}
