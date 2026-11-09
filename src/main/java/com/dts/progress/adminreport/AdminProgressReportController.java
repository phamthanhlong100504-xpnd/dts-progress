package com.dts.progress.adminreport;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Uses only the original question-learning projection and the Card01 completion projection. */
@RestController
@RequestMapping("/api/v1/progress/admin")
public class AdminProgressReportController {
    private final Report report;
    public AdminProgressReportController(Report report){this.report=report;}
    private static <T> ResponseEntity<T> ok(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERM_reports:read')")
    @GetMapping("/analytics/overview")
    public ResponseEntity<Map<String,Object>> overview(@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
        @RequestParam(required=false) String licenseClass,@RequestParam(defaultValue="ALL") String module,
        @RequestParam(required=false) String bankVersion){return ok(report.overview(from,to,licenseClass,module,bankVersion));}
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERM_reports:read')")
    @GetMapping("/analytics/chapters")
    public ResponseEntity<Map<String,Object>> chapters(@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
        @RequestParam(required=false) String licenseClass,@RequestParam(defaultValue="ALL") String module,
        @RequestParam(required=false) String bankVersion){return ok(report.chapters(from,to,licenseClass,module,bankVersion));}
    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERM_integrations:read')")
    @GetMapping("/integrations/progress-sync")
    public ResponseEntity<Map<String,Object>> integration(){return ok(report.integration());}

    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERM_integrations:read')")
    @GetMapping("/integrations/events/{eventId}")
    public ResponseEntity<Map<String,Object>> event(@PathVariable UUID eventId){return ok(report.event(eventId));}

    @Service
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public static class Report {
        private final JdbcTemplate jdbc;
        public Report(JdbcTemplate jdbc){this.jdbc=jdbc;}
        private static java.sql.Timestamp time(Instant value){return java.sql.Timestamp.from(value);}
        private record Scope(Instant from,Instant to,String license,String module,String bank){}
        private static Scope scope(Instant from,Instant to,String license,String module,String bank){
            Instant now=Instant.now(),end=to==null?now:to,start=from==null?end.minus(Duration.ofDays(30)):from;
            if(!start.isBefore(end)||end.isAfter(now.plusSeconds(60))||Duration.between(start,end).compareTo(Duration.ofDays(366))>0)
                throw new IllegalArgumentException("Invalid reporting interval");
            if(!Set.of("ALL","THEORY","SIMULATION").contains(module))throw new IllegalArgumentException("Invalid module");
            if(license!=null && !license.matches("[A-Z0-9]{1,10}"))throw new IllegalArgumentException("Invalid licenseClass");
            if(bank!=null && (bank.length()>96 || !bank.matches("[A-Za-z0-9_\\-.]{1,96}")))throw new IllegalArgumentException("Invalid bankVersion");
            return new Scope(start,end,license,module,bank);
        }
        private boolean complete(){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT backfill_complete FROM simulation_performance_state WHERE singleton",Boolean.class));}
        private static Map<String,Object> meta(Scope s,String status){
            Map<String,Object> m=new LinkedHashMap<>();m.put("from",s.from());m.put("to",s.to());m.put("timezone","Asia/Ho_Chi_Minh");
            m.put("licenseClass",s.license());m.put("bankVersion",s.bank());m.put("module",s.module());m.put("dataStatus",status);
            m.put("asOf",Instant.now());m.put("sourceService","dts-progress");
            if(!status.equals("READY"))m.put("missingSources",List.of("simulation_performance_backfill"));
            return m;
        }
        private String status(Scope s){return s.module().equals("THEORY")||complete()?"READY":"PARTIAL";}
        private List<Map<String,Object>> theory(Scope s,boolean chapters){
            // bankVersion + license scope is kept explicit. No per-learner requests.
            String group=chapters?"chapter_id":"'ALL'::text";
            String sql=("""
                SELECT %s AS group_key,COUNT(DISTINCT user_id)::bigint learners,
                    COUNT(DISTINCT (user_id,bank_version,question_id)) FILTER(WHERE chapter_learned OR critical_learned OR needs_review)::bigint studied_questions,
                    COUNT(DISTINCT (user_id,bank_version,question_id)) FILTER(WHERE needs_review)::bigint need_review,
                    COUNT(DISTINCT (user_id,bank_version,question_id)) FILTER(WHERE is_critical AND critical_learned)::bigint critical_learned
                FROM user_question_progress
                WHERE (?::text IS NULL OR license_class=?) AND (?::text IS NULL OR bank_version=?)
                  AND (last_studied_at>=? AND last_studied_at<? OR last_reviewed_at>=? AND last_reviewed_at<?)
                GROUP BY %s ORDER BY %s
                """).formatted(group,group,group);
            return jdbc.queryForList(sql,
                s.license(),s.license(),s.bank(),s.bank(),time(s.from()),time(s.to()),time(s.from()),time(s.to()));
        }
        private List<Map<String,Object>> simulation(Scope s,boolean chapters){
            String group=chapters?"chapter_id":"'ALL'::text";
            String sql=("""
                WITH in_period AS (
                    SELECT DISTINCT user_id,bank_version,scenario_id
                    FROM simulation_performance_practice_facts
                    WHERE completed_at>=? AND completed_at<?
                      AND (?::text IS NULL OR license_class=?) AND (?::text IS NULL OR bank_version=?)
                ), best AS (
                    SELECT p.user_id,p.bank_version,p.scenario_id,p.chapter_id,
                      MAX(p.score) AS best_score,
                      COUNT(*) FILTER(WHERE p.completed_at>=? AND p.completed_at<?) AS attempts
                    FROM simulation_performance_practice_facts p
                    JOIN in_period a ON a.user_id=p.user_id
                       AND a.bank_version=p.bank_version AND a.scenario_id=p.scenario_id
                    WHERE (?::text IS NULL OR p.license_class=?)
                    GROUP BY p.user_id,p.bank_version,p.scenario_id,p.chapter_id
                )
                SELECT %s AS group_key,COUNT(DISTINCT user_id)::bigint learners,
                  COUNT(*)::bigint practiced_scenarios,
                  COUNT(*) FILTER(WHERE best_score=5)::bigint perfect_scenarios,
                  COUNT(*) FILTER(WHERE best_score<5)::bigint need_review,
                  SUM(attempts)::bigint completed_card01_attempts FROM best GROUP BY %s ORDER BY %s
                """).formatted(group,group,group);
            return jdbc.queryForList(sql,
                time(s.from()),time(s.to()),s.license(),s.license(),s.bank(),s.bank(),time(s.from()),time(s.to()),s.license(),s.license());
        }
        private Map<String,Object> data(Scope s,boolean chapters){
            var out=meta(s,status(s));
            if(!s.module().equals("SIMULATION"))out.put("theory",theory(s,chapters));
            if(!s.module().equals("THEORY"))out.put("simulation",complete()?simulation(s,chapters):null);
            out.put("completionRate",null);
            out.put("warnings",List.of("Bank denominators are owned by source Content Builder; no fabricated completion percentage"));
            return out;
        }
        public Map<String,Object> overview(Instant from,Instant to,String license,String module,String bank){return data(scope(from,to,license,module,bank),false);}
        public Map<String,Object> chapters(Instant from,Instant to,String license,String module,String bank){return data(scope(from,to,license,module,bank),true);}
        public Map<String,Object> event(UUID eventId){
            var rows=jdbc.queryForList("""
                SELECT event_id,event_sequence,event_type,occurred_at,received_at,applied
                FROM simulation_performance_inbox WHERE event_id=?
                """,eventId);
            if(rows.isEmpty())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Event not found");
            var out=new LinkedHashMap<String,Object>();out.put("sourceService","dts-progress");out.put("asOf",Instant.now());
            out.put("dataStatus","READY");out.put("event",rows.getFirst());out.put("projectionApplied",rows.getFirst().get("applied"));
            out.put("brokerAcknowledged",null);return out;
        }
        public Map<String,Object> integration(){
            var out=new LinkedHashMap<String,Object>();out.put("sourceService","dts-progress");out.put("asOf",Instant.now());
            out.put("dataStatus",complete()?"READY":"PARTIAL");
            out.put("simulationStream",jdbc.queryForMap("""
                SELECT (SELECT COUNT(*) FROM simulation_performance_inbox WHERE NOT applied) pending,
                   (SELECT MIN(received_at) FROM simulation_performance_inbox WHERE NOT applied) oldest_pending,
                   (SELECT MAX(received_at) FROM simulation_performance_inbox WHERE applied) last_applied,
                   (SELECT last_sequence FROM simulation_performance_state WHERE singleton) watermark
                """));out.put("brokerAcknowledgedMeansProjectionApplied",false);return out;
        }
    }
}
