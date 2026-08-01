package com.dts.progress.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtValidationProperties(
        String accessSecret,
        String issuer
) {}
