-- Optional demo data: requires MySQL 8+, one existing user and one existing exam.
-- Run only in a disposable database. Generates 3,000 synthetic attempts across 90 days.
SET SESSION cte_max_recursion_depth = 3001;
INSERT INTO exam_attempts
    (user_id, exam_id, status, started_at, submitted_at, score, max_score,
     violation_count, created_id, updated_id, created_at, updated_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 3000
)
SELECT
    (SELECT user_id FROM tbl_users ORDER BY user_id LIMIT 1),
    (SELECT exam_id FROM exams ORDER BY exam_id LIMIT 1),
    CASE WHEN MOD(n, 10) = 0 THEN 'IN_PROGRESS' ELSE 'GRADED' END,
    TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL MOD(n, 90) DAY), MAKETIME(MOD(n, 24), MOD(n * 7, 60), 0)),
    CASE WHEN MOD(n, 10) = 0 THEN NULL
         ELSE TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL MOD(n, 90) DAY), MAKETIME(MOD(n, 24), MOD(n * 7, 60), 0)) END,
    CASE WHEN MOD(n, 10) = 0 THEN NULL ELSE 40 + MOD(n * 13, 61) END,
    100,
    0,
    'report-demo',
    'report-demo',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seq;
