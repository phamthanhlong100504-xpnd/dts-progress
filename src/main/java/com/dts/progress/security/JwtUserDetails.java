package com.dts.progress.security;

import java.util.List;
import java.util.UUID;

public record JwtUserDetails(
        UUID userId,
        String username,
        List<String> roles,
        List<String> permissions
) {}
