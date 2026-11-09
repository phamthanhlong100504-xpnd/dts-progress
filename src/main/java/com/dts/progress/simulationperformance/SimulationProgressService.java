package com.dts.progress.simulationperformance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class SimulationProgressService {
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;private final SimulationEventStore events;
    static final String QUERY=resource();
    public SimulationProgressService(JdbcTemplate jdbc,ObjectMapper mapper,SimulationEventStore events){this.jdbc=jdbc;this.mapper=mapper;this.events=events;}
    private static String resource(){try(var s=new ClassPathResource("simulation-performance/overview.sql").getInputStream()){return new String(s.readAllBytes(),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("Simulation overview query missing",e);}}
    public JsonNode read(UUID user,String license,String bankVersion,Long watermark) {
        SimulationEventStore.requireLicense(license);if(watermark!=null && watermark<1)throw new IllegalArgumentException("Invalid watermark");
        if(bankVersion!=null && !bankVersion.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,95}"))throw new IllegalArgumentException("Invalid bankVersion");
        var marker=events.marker(watermark);String bank=bankVersion!=null?bankVersion:marker.bankVersion();
        JsonNode catalog=bank==null?null:events.catalog(bank,marker.sequence());ObjectNode result;
        if(catalog==null) {
            result=mapper.createObjectNode();for(String f:List.of("totalScenarios","practicedScenarios","completionPercent","perfectScenarios","needReviewCount","bookmarkedScenarios","bookmarkCount","status"))result.putNull(f);
            result.set("chapters",mapper.createArrayNode());result.put("dataStatus","CATALOG_PENDING");
        } else {
            try{result=(ObjectNode)mapper.readTree(jdbc.queryForObject(QUERY,String.class,user,bank,marker.sequence(),catalog.toString()));}
            catch(java.io.IOException e){throw new IllegalStateException("Cannot map simulation overview");}
            result.put("dataStatus",marker.status());
            if(!marker.complete()) {
                for(String f:List.of("practicedScenarios","completionPercent","perfectScenarios","needReviewCount","bookmarkedScenarios","bookmarkCount","status"))result.putNull(f);
                for(JsonNode chapter:result.path("chapters"))for(String f:List.of("practicedScenarios","completionPercent","perfectScenarios","needReviewCount","bookmarkedScenarios","bookmarkCount","status"))((ObjectNode)chapter).putNull(f);
            }
        }
        result.put("licenseClass",license);result.put("bankVersion",bank);result.put("module","SIMULATION");result.put("performanceMode","COMBINED");
        result.put("progressScope","USER_BANK_VERSION_SHARED_LICENSES");result.put("achievementScope","DTS_SIMULATION_120_V1");
        if(marker.asOf()==null)result.putNull("asOf");else result.put("asOf",marker.asOf().toString());
        result.put("simulationReady",marker.bankReady());result.put("sourceWatermark",marker.sequence());result.set("syncStatus",mapper.valueToTree(marker));
        return result;
    }
}
