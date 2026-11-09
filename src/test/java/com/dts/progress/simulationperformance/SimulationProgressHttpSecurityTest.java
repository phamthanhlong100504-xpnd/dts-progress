package com.dts.progress.simulationperformance;
import com.dts.progress.config.*;
import com.dts.progress.security.JwtValidationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=SimulationProgressController.class,properties={"jwt.access-secret=c2NyZWVuMTQtMTYtaHR0cC10ZXN0LWtleS1vbmx5LTMya2V5cy0wMDA=","jwt.issuer=dts-identity-service"})
@Import({SecurityConfig.class,JwtValidationFilter.class})
@EnableConfigurationProperties(JwtValidationProperties.class)
class SimulationProgressHttpSecurityTest {
    static final String PATH="/api/v1/progress/simulations/learning-overview";
    @Autowired MockMvc http;@MockBean SimulationProgressService service;
    final UUID owner=UUID.randomUUID(),other=UUID.randomUUID();
    String token(String issuer,List<String> grants){return Jwts.builder().subject(owner.toString()).issuer(issuer).claim("userId",other.toString()).claim("permissions",grants).expiration(Date.from(Instant.now().plusSeconds(600))).signWith(Keys.hmacShaKeyFor("screen14-16-http-test-key-only-32keys-000".getBytes(StandardCharsets.UTF_8))).compact();}
    @Test void anonymousAndInvalidIssuerAreDenied()throws Exception{
        http.perform(get(PATH).param("licenseClass","B")).andExpect(status().isUnauthorized());
        http.perform(get(PATH).param("licenseClass","B").header("Authorization","Bearer "+token("wrong",List.of("progress:read","simulations:read")))).andExpect(status().isUnauthorized());verifyNoInteractions(service);
    }
    @Test void bothIndependentPermissionsAreRequired()throws Exception{
        for(var grants:List.of(List.<String>of(),List.of("progress:read"),List.of("simulations:read")))
            http.perform(get(PATH).param("licenseClass","B").header("Authorization","Bearer "+token("dts-identity-service",grants))).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @Test void authenticatedSubjectOwnsTheReadAndGetHasNoEventSideEffect()throws Exception{
        when(service.read(eq(owner),eq("B"),isNull(),isNull())).thenReturn(new ObjectMapper().createObjectNode().put("bookmarkedScenarios",3));
        http.perform(get(PATH).param("licenseClass","B").param("userId",other.toString()).header("X-User-ID",other.toString()).header("Authorization","Bearer "+token("dts-identity-service",List.of("progress:read","simulations:read"))))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.bookmarkedScenarios").value(3));
        verify(service).read(owner,"B",null,null);verifyNoMoreInteractions(service);
    }
}
