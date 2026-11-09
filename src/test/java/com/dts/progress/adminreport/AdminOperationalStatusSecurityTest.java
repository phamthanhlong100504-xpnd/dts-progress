package com.dts.progress.adminreport;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import static org.junit.jupiter.api.Assertions.*;

class AdminOperationalStatusSecurityTest {
    @Test void ownerStatusDoesNotInventUnavailableDependenciesOrLeakyCaching() {
        var source=org.mockito.Mockito.mock(AdminProgressReportController.Report.class);
        org.mockito.Mockito.when(source.integration()).thenReturn(java.util.Map.of("dataStatus","READY"));
        var reply=new AdminOperationalStatusController(source).status();
        assertEquals("PARTIAL",reply.getBody().get("dataStatus"));
        assertNull(reply.getBody().get("practiceProducerStatus"));
        assertEquals("no-store",reply.getHeaders().getFirst("Cache-Control"));
        org.mockito.Mockito.verify(source).integration();
    }
    @Test void operationHasIndependentSystemStatusGrant() throws Exception {
        var method=AdminOperationalStatusController.class.getMethod("status");
        var policy=method.getAnnotation(PreAuthorize.class);
        assertNotNull(policy);
        assertTrue(policy.value().contains("ROLE_ADMIN"));
        assertTrue(policy.value().contains("PERM_system-status:read"));
        assertFalse(policy.value().contains("PERM_reports:read"));
        assertFalse(policy.value().contains("PERM_integrations:read"));
        assertEquals("/api/v1/progress/admin/operations/status",method.getAnnotation(GetMapping.class).value()[0]);
        assertEquals(ResponseEntity.class,method.getReturnType());
    }
}
