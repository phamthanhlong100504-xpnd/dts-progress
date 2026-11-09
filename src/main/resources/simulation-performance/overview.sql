WITH request AS(SELECT ?::uuid who,?::text bank,?::bigint watermark,?::jsonb catalog),
scenarios AS(SELECT(s->>'scenarioId')::uuid id,(s->>'scenarioNumber')::integer number,(s->>'chapterId')::integer chapter_id
 FROM request q CROSS JOIN LATERAL jsonb_array_elements(q.catalog->'scenarios')s),
practiced AS(SELECT a.scenario_id,MAX(a.score) best_score,MAX(a.completed_at) last_studied_at
 FROM simulation_performance_practice_facts a CROSS JOIN request q
 WHERE a.user_id=q.who AND a.bank_version=q.bank AND a.event_sequence<=q.watermark GROUP BY a.scenario_id),
marks AS(SELECT DISTINCT ON((e.data->>'scenarioId')::uuid)(e.data->>'scenarioId')::uuid scenario_id,(e.data->>'marked')::boolean marked
 FROM simulation_performance_inbox e CROSS JOIN request q WHERE e.applied AND e.event_type='SIMULATION_BOOKMARK_CHANGED'
 AND e.user_id=q.who AND e.bank_version=q.bank AND e.event_sequence<=q.watermark
 ORDER BY(e.data->>'scenarioId')::uuid,e.event_sequence DESC),
states AS(SELECT s.*,p.best_score,p.last_studied_at,COALESCE(m.marked,FALSE) marked
 FROM scenarios s LEFT JOIN practiced p ON p.scenario_id=s.id LEFT JOIN marks m ON m.scenario_id=s.id),
chapters AS(SELECT(c->>'chapterId')::integer id,c->>'name' name,(c->>'totalScenarios')::integer total FROM request q CROSS JOIN LATERAL jsonb_array_elements(q.catalog->'chapters')c),
by_chapter AS(SELECT c.id,c.name,c.total,COUNT(*)FILTER(WHERE s.best_score IS NOT NULL)::integer practiced,
 COUNT(*)FILTER(WHERE s.best_score=5)::integer perfect,COUNT(*)FILTER(WHERE s.best_score<5)::integer need_review,
 COUNT(*)FILTER(WHERE s.marked)::integer bookmarks,MAX(s.last_studied_at) last_studied_at
 FROM chapters c LEFT JOIN states s ON s.chapter_id=c.id GROUP BY c.id,c.name,c.total),
overall AS(SELECT COUNT(*)::integer total,COUNT(*)FILTER(WHERE best_score IS NOT NULL)::integer practiced,
 COUNT(*)FILTER(WHERE best_score=5)::integer perfect,COUNT(*)FILTER(WHERE best_score<5)::integer need_review,
 COUNT(*)FILTER(WHERE marked)::integer bookmarks FROM states)
SELECT jsonb_build_object('totalScenarios',o.total,'practicedScenarios',o.practiced,'perfectScenarios',o.perfect,
 'needReviewCount',o.need_review,'bookmarkedScenarios',o.bookmarks,'bookmarkCount',o.bookmarks,
 'completionPercent',ROUND(o.practiced::numeric*100/NULLIF(o.total,0),0),
 'status',CASE WHEN o.practiced=0 THEN 'NOT_STARTED' WHEN o.practiced=o.total THEN 'COMPLETED' ELSE 'IN_PROGRESS' END,
 'chapters',COALESCE((SELECT jsonb_agg(jsonb_build_object('chapterId',c.id,'chapterName',c.name,'totalScenarios',c.total,
 'practicedScenarios',c.practiced,'completionPercent',ROUND(c.practiced::numeric*100/c.total,0),'needReviewCount',c.need_review,
 'perfectScenarios',c.perfect,'bookmarkedScenarios',c.bookmarks,'bookmarkCount',c.bookmarks,'lastStudiedAt',c.last_studied_at,
 'status',CASE WHEN c.practiced=0 THEN 'NOT_STARTED' WHEN c.practiced=c.total THEN 'COMPLETED' ELSE 'IN_PROGRESS' END,
 'navigation',jsonb_build_object('targetKind','SIMULATION_CHAPTER','chapterId',c.id,'bankVersion',(SELECT bank FROM request),
  'path','/practice/simulations/chapters/'||c.id))ORDER BY c.id)FROM by_chapter c),'[]'::jsonb))::text FROM overall o
