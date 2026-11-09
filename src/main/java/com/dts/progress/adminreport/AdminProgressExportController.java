package com.dts.progress.adminreport;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Uses the same bounded scope and reporting permission as Progress Admin analytics. */
@RestController
@RequestMapping("/api/v1/progress/admin/analytics")
public class AdminProgressExportController {
    private final AdminProgressReportController.Report report;
    public AdminProgressExportController(AdminProgressReportController.Report report){this.report=report;}
    @GetMapping(value="/export",produces="text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERM_reports:read')")
    public ResponseEntity<byte[]> export(@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
            @RequestParam(required=false) String licenseClass,@RequestParam(defaultValue="ALL") String module,
            @RequestParam(required=false) String bankVersion){
        Map<String,Object> summary=report.overview(from,to,licenseClass,module,bankVersion);
        if(!"READY".equals(summary.get("dataStatus")))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Progress projection not ready");
        Map<String,Object> details=report.chapters(from,to,licenseClass,module,bankVersion);
        if(!"READY".equals(details.get("dataStatus")))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Progress chapter projection not ready");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
          .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"dts-admin-progress.csv\"")
          .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
          .body(AdminProgressCsv.encode(summary,details));
    }
}
