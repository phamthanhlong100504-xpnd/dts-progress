package com.dts.progress.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Maps verified Identity claims to Spring authorities without granting defaults. */
public final class JwtAuthorityMapper {

    private JwtAuthorityMapper() {}

    public static List<String> authorityNames(List<?> roles, List<?> permissions) {
        Set<String> names = new LinkedHashSet<>();
        if (roles != null) {
            for (Object value : roles) {
                if (value instanceof String role && !role.isBlank()) {
                    String name = role.trim();
                    names.add(name.startsWith("ROLE_") ? name : "ROLE_" + name);
                }
            }
        }
        if (permissions != null) {
            for (Object value : permissions) {
                if (value instanceof String permission && !permission.isBlank()) {
                    names.add("PERM_" + permission.trim());
                }
            }
        }
        return List.copyOf(names);
    }
}
