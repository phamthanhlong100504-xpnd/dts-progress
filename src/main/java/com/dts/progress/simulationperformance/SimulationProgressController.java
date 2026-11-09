package com.dts.progress.simulationperformance;

import com.dts.progress.dto.response.ApiResponse;
import com.dts.progress.security.JwtUserDetails;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/progress/simulations/learning-overview")
public class SimulationProgressController {
    private final SimulationProgressService service;
    public SimulationProgressController(SimulationProgressService service){this.service=service;}
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_progress:read') and hasAuthority('PERM_simulations:read')")
    public ResponseEntity<ApiResponse<JsonNode>> read(@AuthenticationPrincipal JwtUserDetails user,@RequestParam String licenseClass,
            @RequestParam(required=false) String bankVersion,@RequestParam(required=false) Long watermark) {
        if(user==null)throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(service.read(user.userId(),licenseClass,bankVersion,watermark)));
    }
}
