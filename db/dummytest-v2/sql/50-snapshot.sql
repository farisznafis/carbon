-- Static EN-R-002 dummytest-v2 data. Run only on a dedicated test database.
BEGIN;
DO $$ BEGIN IF (SELECT count(*) FROM en_r002_equipment WHERE id BETWEEN 1 AND 8)<>8 THEN RAISE EXCEPTION 'Run earlier stages first'; END IF; IF EXISTS (SELECT 1 FROM batch_process_history WHERE claimed_by='fixture-snapshot') THEN RAISE EXCEPTION '50-snapshot already applied'; END IF; END $$;
-- batch_process_history
INSERT INTO batch_process_history (id, is_active, created_at, updated_at, created_by, updated_by, start_time, end_time, status, claimed_by, claimed_at)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '2026-09-16 08:05:01', '2026-09-16 08:05:31', 'COMPLETED', 'fixture-snapshot', '2026-09-16 08:05:01'),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '2026-10-01 08:05:01', '2026-10-01 08:05:31', 'COMPLETED', 'fixture-snapshot', '2026-10-01 08:05:01');

-- en_r002_reduction_result
INSERT INTO en_r002_reduction_result (id, is_active, created_at, updated_at, created_by, updated_by, batch_id, processing_date, project_id, start_time, end_time, site_id, equipment_id, elpj, elbl, embl, empj, empj_m, empj_s, empj_s_control, empj_s_battery, er, deleted_at, deleted_by)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, '2026-09-15', 1, '2026-09-16 08:05:01', '2026-09-16 08:05:31', 1, 1, 980.00, 980.00, 0.502, 0.019, 0, 0.019, 0.013, 0.006, 0.483, NULL, NULL),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, '2026-09-15', 1, '2026-09-16 08:05:01', '2026-09-16 08:05:31', 2, 2, 1120.00, 1120.00, 0.573, 0.021, 0, 0.021, 0.014, 0.007, 0.552, NULL, NULL),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, '2026-09-15', 1, '2026-09-16 08:05:01', '2026-09-16 08:05:31', 3, 3, 1260.00, 1260.00, 0.645, 0.016, 0, 0.016, 0.016, 0.000, 0.629, NULL, NULL),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, '2026-09-15', 1, '2026-09-16 08:05:01', '2026-09-16 08:05:31', 4, 4, 1400.00, 1400.00, 0.717, 0.027, 0, 0.027, 0.018, 0.009, 0.690, NULL, NULL),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, '2026-09-15', 1, '2026-09-16 08:05:01', '2026-09-16 08:05:31', 5, 5, 1540.00, 1540.00, 0.788, 0.030, 0, 0.030, 0.020, 0.010, 0.758, NULL, NULL),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, '2026-09-30', 1, '2026-10-01 08:05:01', '2026-10-01 08:05:31', 1, 1, 1876.00, 1876.00, 0.961, 0.036, 0, 0.036, 0.024, 0.012, 0.925, NULL, NULL),
(7, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, '2026-09-30', 1, '2026-10-01 08:05:01', '2026-10-01 08:05:31', 2, 2, 2144.00, 2144.00, 1.097, 0.041, 0, 0.041, 0.027, 0.014, 1.056, NULL, NULL),
(8, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, '2026-09-30', 1, '2026-10-01 08:05:01', '2026-10-01 08:05:31', 3, 3, 2412.00, 2412.00, 1.235, 0.031, 0, 0.031, 0.031, 0.000, 1.204, NULL, NULL),
(9, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, '2026-09-30', 1, '2026-10-01 08:05:01', '2026-10-01 08:05:31', 4, 4, 2680.00, 2680.00, 1.372, 0.051, 0, 0.051, 0.034, 0.017, 1.321, NULL, NULL),
(10, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, '2026-09-30', 1, '2026-10-01 08:05:01', '2026-10-01 08:05:31', 5, 5, 2948.00, 2948.00, 1.509, 0.057, 0, 0.057, 0.038, 0.019, 1.452, NULL, NULL);

-- en_r002_reduction_result_total
INSERT INTO en_r002_reduction_result_total (id, is_active, created_at, updated_at, created_by, updated_by, batch_id, processing_date, project_id, start_time, end_time, elpj, elbl, embl, empj, empj_m, empj_s, empj_s_control, empj_s_battery, er, deleted_at, deleted_by)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, '2026-09-30', 1, '2026-09-16 08:05:01', '2026-10-01 08:05:31', 12060.00, 12060.00, 6.174, 0.216, 0, 0.216, 0.154, 0.062, 5.958, NULL, NULL);

-- Form-complete output fixtures associated with the P01 snapshot.
INSERT INTO project_files (id,is_active,created_at,updated_at,created_by,updated_by,project_id,deleted_at,deleted_by,file_type,s3_key,original_file_name,owned_by,file_version) OVERRIDING SYSTEM VALUE VALUES
(7,TRUE,'2026-10-09 09:00:00','2026-10-09 09:00:00','it.admin','it.admin',1,NULL,NULL,'ACTIVITY_RESULT_LIST','dummytest-v2/enr002/individual-activity/P01-individual-activity.xlsx','P01-individual-activity.xlsx',TRUE,1),
(8,TRUE,'2026-10-09 09:00:00','2026-10-09 09:00:00','it.admin','it.admin',1,NULL,NULL,'MONITORING_REPORT','dummytest-v2/enr002/houkoku/P01-monitoring-report.xlsx','P01-monitoring-report.xlsx',TRUE,1);
SELECT setval(pg_get_serial_sequence('project_files','id'), (SELECT MAX(id) FROM project_files), true);

SELECT setval(pg_get_serial_sequence('batch_process_history','id'), (SELECT MAX(id) FROM batch_process_history), true);
SELECT setval(pg_get_serial_sequence('en_r002_reduction_result','id'), (SELECT MAX(id) FROM en_r002_reduction_result), true);
SELECT setval(pg_get_serial_sequence('en_r002_reduction_result_total','id'), (SELECT MAX(id) FROM en_r002_reduction_result_total), true);
COMMIT;
