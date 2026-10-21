-- Static EN-R-002 dummytest-v2 data. Run only on a dedicated test database.
SELECT 'operator_company' AS entity, count(*) AS actual, 1 AS expected FROM operator_company WHERE company_id='OP000000001'
UNION ALL SELECT 'project',count(*),2 FROM project WHERE public_id IN ('ITPROJ001','ITPROJ002')
UNION ALL SELECT 'company',count(*),6 FROM participating_company WHERE company_id BETWEEN 'PC000000001' AND 'PC000000006'
UNION ALL SELECT 'site',count(*),8 FROM site WHERE public_id BETWEEN 'ITSITE001' AND 'ITSITE008'
UNION ALL SELECT 'equipment',count(*),8 FROM en_r002_equipment WHERE device_no LIKE 'IT-S%-PCS-%'
UNION ALL SELECT 'participation',count(*),8 FROM participating_project WHERE id BETWEEN 1 AND 8
UNION ALL SELECT 'approved',count(*),5 FROM participating_project WHERE id BETWEEN 1 AND 8 AND status='APPROVED'
UNION ALL SELECT 'snapshot-results',count(*),10 FROM en_r002_reduction_result WHERE equipment_id BETWEEN 1 AND 5;

DO $$ BEGIN
 IF EXISTS (SELECT device_no FROM en_r002_equipment WHERE device_no LIKE 'IT-S%-PCS-%' GROUP BY device_no HAVING count(*)>1) THEN RAISE EXCEPTION 'Duplicate device_no'; END IF;
 IF EXISTS (SELECT 1 FROM participating_project pp LEFT JOIN en_r002_equipment e ON e.id=pp.equipment_id WHERE pp.id BETWEEN 1 AND 8 AND e.id IS NULL) THEN RAISE EXCEPTION 'Orphan participation'; END IF;
END $$;
