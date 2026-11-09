package com.dts.progress.adminreport;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.junit.jupiter.api.Assertions.*;

class AdminProgressSecurityTest {
    @Test void allAdminEndpointsRequireRoleAndReportPermission(){
        for(var method:AdminProgressReportController.class.getDeclaredMethods()){
            if(!Modifier.isPublic(method.getModifiers())||method.getName().equals("ok"))continue;
            PreAuthorize check=method.getAnnotation(PreAuthorize.class);
            assertNotNull(check,method.getName());assertTrue(check.value().contains("ROLE_ADMIN"));
            assertTrue(check.value().contains(method.getName().equals("integration") || method.getName().equals("event")
                ? "PERM_integrations:read" : "PERM_reports:read"));
        }
    }
}
