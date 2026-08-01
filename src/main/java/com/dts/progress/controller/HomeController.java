package com.dts.progress.controller;

import com.dts.progress.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> home() {
        return ApiResponse.ok(Map.of(
                "service", "DTS Progress Service",
                "version", "1.0.0",
                "status", "running"
        ));
    }
}
