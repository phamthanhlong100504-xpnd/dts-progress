package com.dts.progress.controller;

import com.dts.progress.dto.request.UpdateProgramRequest;
import com.dts.progress.security.JwtUserDetails;
import com.dts.progress.service.UserPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/progress/preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @PutMapping("/learning-program")
    public ResponseEntity<Void> updateProgram(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestBody UpdateProgramRequest request) {
        
        userPreferenceService.updateLearningProgram(
            userDetails.userId().toString(), 
            request.getProgramCode()
        );
        
        return ResponseEntity.ok().build();
    }
}
