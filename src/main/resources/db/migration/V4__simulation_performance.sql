-- Separate simulation inbox/projection; no legacy results, EXP function or progress rows are changed.
CREATE TABLE simulation_performance_inbox (
 event_sequence BIGINT PRIMARY KEY CHECK(event_sequence>0),event_id UUID NOT NULL UNIQUE,
 event_type TEXT NOT NULL,occurred_at TIMESTAMPTZ NOT NULL,data JSONB NOT NULL,
 user_id UUID,license_class VARCHAR(16),source_id UUID,bank_version VARCHAR(96),
 applied BOOLEAN NOT NULL DEFAULT FALSE,received_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX simulation_inbox_scope ON simulation_performance_inbox(user_id,license_class,event_type,occurred_at,event_sequence);
CREATE INDEX simulation_inbox_bank ON simulation_performance_inbox(user_id,bank_version,event_type,event_sequence);
CREATE INDEX simulation_inbox_source ON simulation_performance_inbox(event_type,source_id);
CREATE TABLE simulation_performance_state (
 singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK(singleton),last_sequence BIGINT NOT NULL DEFAULT 0,
 capture_started_at TIMESTAMPTZ,backfill_complete BOOLEAN NOT NULL DEFAULT FALSE
);
INSERT INTO simulation_performance_state(singleton) VALUES(TRUE);
CREATE FUNCTION simulation_projection_evidence_guard() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND ROW(NEW.event_sequence,NEW.event_id,NEW.event_type,NEW.occurred_at,NEW.data,NEW.user_id,NEW.license_class,NEW.source_id,NEW.bank_version,NEW.received_at)
 IS NOT DISTINCT FROM ROW(OLD.event_sequence,OLD.event_id,OLD.event_type,OLD.occurred_at,OLD.data,OLD.user_id,OLD.license_class,OLD.source_id,OLD.bank_version,OLD.received_at)
 AND NOT OLD.applied AND NEW.applied THEN RETURN NEW;END IF;
 RAISE EXCEPTION 'Simulation projection evidence cannot be replaced or deleted';
END $$;
CREATE TRIGGER simulation_projection_evidence_guard BEFORE UPDATE OR DELETE ON simulation_performance_inbox FOR EACH ROW EXECUTE FUNCTION simulation_projection_evidence_guard();

CREATE VIEW simulation_performance_exam_facts AS
 SELECT DISTINCT ON(source_id) event_sequence,event_id,user_id,license_class,source_id,
  data->>'activityType' activity_type,data->>'mode' mode,data->>'displayTitle' display_title,
  (data->>'score')::integer source_score,50::integer source_max_score,data->>'result' result,
  (data->>'completedAt')::timestamptz completed_at,(data->>'startedAt')::timestamptz started_at,
  (data->>'durationSeconds')::bigint duration_seconds,bank_version,data->>'scoringVersion' scoring_version,
  (data->>'presetSetId')::uuid preset_set_id,(data->>'presetVersionId')::uuid preset_version_id,
  data->>'expPolicyVersion' exp_policy_version,(data->>'expPolicyRevision')::bigint exp_policy_revision,
  (data->>'nSnapshot')::integer n_snapshot,(data->>'expEligible')::boolean exp_eligible,
  data->>'expExclusionReason' exp_exclusion_reason
 FROM simulation_performance_inbox WHERE applied AND event_type='SIMULATION_EXAM_COMPLETED'
 ORDER BY source_id,event_sequence;
CREATE VIEW simulation_performance_practice_facts AS
 SELECT DISTINCT ON(source_id) event_sequence,event_id,user_id,license_class,source_id,bank_version,
  (data->>'scenarioId')::uuid scenario_id,(data->>'scenarioNumber')::integer scenario_number,
  (data->>'chapterId')::integer chapter_id,(data->>'score')::integer score,
  (data->>'completedAt')::timestamptz completed_at,data->>'achievementScope' achievement_scope,
  data->>'expPolicyVersion' exp_policy_version,(data->>'firstPerfectEligible')::boolean first_perfect_eligible
 FROM simulation_performance_inbox WHERE applied AND event_type='SIMULATION_PRACTICE_COMPLETED'
 ORDER BY source_id,event_sequence;
