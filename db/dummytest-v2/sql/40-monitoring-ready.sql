-- Static EN-R-002 dummytest-v2 data. Run only on a dedicated test database.
BEGIN;
DO $$ BEGIN
 IF (SELECT count(*) FROM participating_project WHERE id BETWEEN 1 AND 8) <> 8 THEN RAISE EXCEPTION 'Run 30-participations.sql first'; END IF;
 IF (SELECT count(*) FROM participating_project WHERE status='APPROVED' AND id BETWEEN 1 AND 8) <> 5 THEN RAISE EXCEPTION 'Expected five approved memberships'; END IF;
END $$;
-- Monitoring records are DynamoDB data. Import monitoring-wave-1.json and then monitoring-wave-2.json as documented.
COMMIT;
