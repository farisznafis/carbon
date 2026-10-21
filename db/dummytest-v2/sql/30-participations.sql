-- Static EN-R-002 dummytest-v2 data. Run only on a dedicated test database.
BEGIN;
DO $$ BEGIN IF (SELECT count(*) FROM participating_company WHERE company_id BETWEEN 'PC000000001' AND 'PC000000006')<>6 THEN RAISE EXCEPTION 'Run 20-companies.sql first'; END IF; IF EXISTS (SELECT 1 FROM en_r002_equipment WHERE device_no LIKE 'IT-S%-PCS-%') THEN RAISE EXCEPTION '30-participations already applied'; END IF; END $$;
-- site_project_relation
INSERT INTO site_project_relation (id, is_active, created_at, updated_at, created_by, updated_by, project_id, site_id, activity_start_date, deleted_at, deleted_by)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, 1, '2026-08-01', NULL, NULL),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, 2, '2026-08-01', NULL, NULL),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, 3, '2026-08-01', NULL, NULL),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, 4, '2026-08-01', NULL, NULL),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, 5, '2026-08-01', NULL, NULL);

-- en_r002_equipment
INSERT INTO en_r002_equipment (id, is_active, created_at, updated_at, created_by, updated_by, deleted_at, deleted_by, site_id, change_date, previous_equipment_id, equipment_op_date, nominal_max_output, pcs_manufacturer, pcs_model, device_no, pcs_standby, has_battery, battery_maker, battery_model, battery_capacity, total_investment, has_subsidy, subsidy_name, subsidy_amount, pre_project_unit_price)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', NULL, NULL, 1, NULL, NULL, '2026-08-01', 35, 'Test Solar Systems', 'TS-PCS-35', 'IT-S01-PCS-1', 0.30, TRUE, 'Test Storage', 'TS-BAT-40', 40, 7700000, FALSE, NULL, NULL, 28.50),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', NULL, NULL, 2, NULL, NULL, '2026-08-01', 40, 'Test Solar Systems', 'TS-PCS-40', 'IT-S02-PCS-1', 0.30, TRUE, 'Test Storage', 'TS-BAT-40', 40, 8800000, TRUE, '地域再生可能エネルギー導入補助金', 1600000, 28.50),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', NULL, NULL, 3, NULL, NULL, '2026-08-01', 45, 'Test Solar Systems', 'TS-PCS-45', 'IT-S03-PCS-1', 0.30, FALSE, NULL, NULL, NULL, 9900000, FALSE, NULL, NULL, 28.50),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', NULL, NULL, 4, NULL, NULL, '2026-08-01', 50, 'Test Solar Systems', 'TS-PCS-50', 'IT-S04-PCS-1', 0.30, TRUE, 'Test Storage', 'TS-BAT-40', 40, 11000000, TRUE, '地域再生可能エネルギー導入補助金', 2000000, 28.50),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', NULL, NULL, 5, NULL, NULL, '2026-08-01', 55, 'Test Solar Systems', 'TS-PCS-55', 'IT-S05-PCS-1', 0.30, TRUE, 'Test Storage', 'TS-BAT-40', 40, 12100000, FALSE, NULL, NULL, 28.50),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', NULL, NULL, 6, NULL, NULL, '2026-08-01', 60, 'Test Solar Systems', 'TS-PCS-60', 'IT-S06-PCS-1', 0.30, FALSE, NULL, NULL, NULL, 13200000, TRUE, '地域再生可能エネルギー導入補助金', 2400000, 28.50),
(7, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', NULL, NULL, 7, NULL, NULL, '2026-08-01', 65, 'Test Solar Systems', 'TS-PCS-65', 'IT-S07-PCS-1', 0.30, TRUE, 'Test Storage', 'TS-BAT-40', 40, 14300000, FALSE, NULL, NULL, 28.50),
(8, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', NULL, NULL, 8, NULL, NULL, '2026-08-01', 70, 'Test Solar Systems', 'TS-PCS-70', 'IT-S08-PCS-1', 0.30, TRUE, 'Test Storage', 'TS-BAT-40', 40, 15400000, TRUE, '地域再生可能エネルギー導入補助金', 2800000, 28.50);

-- en_r002_equipment_file
INSERT INTO en_r002_equipment_file (id, is_active, created_at, updated_at, created_by, updated_by, equipment_id, deleted_at, deleted_by, file_type, file_id, s3_key)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'SOLAR_CONTRACT', 'it-e01-solar_contract', 'dummytest-v2/enr002/equipment/E01/solar_contract.pdf'),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'PANEL_CATALOG', 'it-e01-panel_catalog', 'dummytest-v2/enr002/equipment/E01/panel_catalog.pdf'),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'PCS_SPEC', 'it-e01-pcs_spec', 'dummytest-v2/enr002/equipment/E01/pcs_spec.pdf'),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'CONSTRUCTION_INVOICE', 'it-e01-construction_invoice', 'dummytest-v2/enr002/equipment/E01/construction_invoice.pdf'),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'ELECTRICITY_BILL', 'it-e01-electricity_bill', 'dummytest-v2/enr002/equipment/E01/electricity_bill.pdf'),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'BATTERY_CATALOG', 'it-e01-battery_catalog', 'dummytest-v2/enr002/equipment/E01/battery_catalog.pdf'),
(7, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'SOLAR_CONTRACT', 'it-e02-solar_contract', 'dummytest-v2/enr002/equipment/E02/solar_contract.pdf'),
(8, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'PANEL_CATALOG', 'it-e02-panel_catalog', 'dummytest-v2/enr002/equipment/E02/panel_catalog.pdf'),
(9, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'PCS_SPEC', 'it-e02-pcs_spec', 'dummytest-v2/enr002/equipment/E02/pcs_spec.pdf'),
(10, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'CONSTRUCTION_INVOICE', 'it-e02-construction_invoice', 'dummytest-v2/enr002/equipment/E02/construction_invoice.pdf'),
(11, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'ELECTRICITY_BILL', 'it-e02-electricity_bill', 'dummytest-v2/enr002/equipment/E02/electricity_bill.pdf'),
(12, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'BATTERY_CATALOG', 'it-e02-battery_catalog', 'dummytest-v2/enr002/equipment/E02/battery_catalog.pdf'),
(13, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'SUBSIDY_NOTICE', 'it-e02-subsidy_notice', 'dummytest-v2/enr002/equipment/E02/subsidy_notice.pdf'),
(14, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 3, NULL, NULL, 'SOLAR_CONTRACT', 'it-e03-solar_contract', 'dummytest-v2/enr002/equipment/E03/solar_contract.pdf'),
(15, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 3, NULL, NULL, 'PANEL_CATALOG', 'it-e03-panel_catalog', 'dummytest-v2/enr002/equipment/E03/panel_catalog.pdf'),
(16, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 3, NULL, NULL, 'PCS_SPEC', 'it-e03-pcs_spec', 'dummytest-v2/enr002/equipment/E03/pcs_spec.pdf'),
(17, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 3, NULL, NULL, 'CONSTRUCTION_INVOICE', 'it-e03-construction_invoice', 'dummytest-v2/enr002/equipment/E03/construction_invoice.pdf'),
(18, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 3, NULL, NULL, 'ELECTRICITY_BILL', 'it-e03-electricity_bill', 'dummytest-v2/enr002/equipment/E03/electricity_bill.pdf'),
(19, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 4, NULL, NULL, 'SOLAR_CONTRACT', 'it-e04-solar_contract', 'dummytest-v2/enr002/equipment/E04/solar_contract.pdf'),
(20, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 4, NULL, NULL, 'PANEL_CATALOG', 'it-e04-panel_catalog', 'dummytest-v2/enr002/equipment/E04/panel_catalog.pdf'),
(21, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 4, NULL, NULL, 'PCS_SPEC', 'it-e04-pcs_spec', 'dummytest-v2/enr002/equipment/E04/pcs_spec.pdf'),
(22, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 4, NULL, NULL, 'CONSTRUCTION_INVOICE', 'it-e04-construction_invoice', 'dummytest-v2/enr002/equipment/E04/construction_invoice.pdf'),
(23, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 4, NULL, NULL, 'ELECTRICITY_BILL', 'it-e04-electricity_bill', 'dummytest-v2/enr002/equipment/E04/electricity_bill.pdf'),
(24, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 4, NULL, NULL, 'BATTERY_CATALOG', 'it-e04-battery_catalog', 'dummytest-v2/enr002/equipment/E04/battery_catalog.pdf'),
(25, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 4, NULL, NULL, 'SUBSIDY_NOTICE', 'it-e04-subsidy_notice', 'dummytest-v2/enr002/equipment/E04/subsidy_notice.pdf'),
(26, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 5, NULL, NULL, 'SOLAR_CONTRACT', 'it-e05-solar_contract', 'dummytest-v2/enr002/equipment/E05/solar_contract.pdf'),
(27, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 5, NULL, NULL, 'PANEL_CATALOG', 'it-e05-panel_catalog', 'dummytest-v2/enr002/equipment/E05/panel_catalog.pdf'),
(28, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 5, NULL, NULL, 'PCS_SPEC', 'it-e05-pcs_spec', 'dummytest-v2/enr002/equipment/E05/pcs_spec.pdf'),
(29, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 5, NULL, NULL, 'CONSTRUCTION_INVOICE', 'it-e05-construction_invoice', 'dummytest-v2/enr002/equipment/E05/construction_invoice.pdf'),
(30, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 5, NULL, NULL, 'ELECTRICITY_BILL', 'it-e05-electricity_bill', 'dummytest-v2/enr002/equipment/E05/electricity_bill.pdf'),
(31, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 5, NULL, NULL, 'BATTERY_CATALOG', 'it-e05-battery_catalog', 'dummytest-v2/enr002/equipment/E05/battery_catalog.pdf'),
(32, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 6, NULL, NULL, 'SOLAR_CONTRACT', 'it-e06-solar_contract', 'dummytest-v2/enr002/equipment/E06/solar_contract.pdf'),
(33, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 6, NULL, NULL, 'PANEL_CATALOG', 'it-e06-panel_catalog', 'dummytest-v2/enr002/equipment/E06/panel_catalog.pdf'),
(34, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 6, NULL, NULL, 'PCS_SPEC', 'it-e06-pcs_spec', 'dummytest-v2/enr002/equipment/E06/pcs_spec.pdf'),
(35, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 6, NULL, NULL, 'CONSTRUCTION_INVOICE', 'it-e06-construction_invoice', 'dummytest-v2/enr002/equipment/E06/construction_invoice.pdf'),
(36, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 6, NULL, NULL, 'ELECTRICITY_BILL', 'it-e06-electricity_bill', 'dummytest-v2/enr002/equipment/E06/electricity_bill.pdf'),
(37, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 6, NULL, NULL, 'SUBSIDY_NOTICE', 'it-e06-subsidy_notice', 'dummytest-v2/enr002/equipment/E06/subsidy_notice.pdf'),
(38, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 7, NULL, NULL, 'SOLAR_CONTRACT', 'it-e07-solar_contract', 'dummytest-v2/enr002/equipment/E07/solar_contract.pdf'),
(39, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 7, NULL, NULL, 'PANEL_CATALOG', 'it-e07-panel_catalog', 'dummytest-v2/enr002/equipment/E07/panel_catalog.pdf'),
(40, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 7, NULL, NULL, 'PCS_SPEC', 'it-e07-pcs_spec', 'dummytest-v2/enr002/equipment/E07/pcs_spec.pdf'),
(41, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 7, NULL, NULL, 'CONSTRUCTION_INVOICE', 'it-e07-construction_invoice', 'dummytest-v2/enr002/equipment/E07/construction_invoice.pdf'),
(42, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 7, NULL, NULL, 'ELECTRICITY_BILL', 'it-e07-electricity_bill', 'dummytest-v2/enr002/equipment/E07/electricity_bill.pdf'),
(43, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 7, NULL, NULL, 'BATTERY_CATALOG', 'it-e07-battery_catalog', 'dummytest-v2/enr002/equipment/E07/battery_catalog.pdf'),
(44, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 8, NULL, NULL, 'SOLAR_CONTRACT', 'it-e08-solar_contract', 'dummytest-v2/enr002/equipment/E08/solar_contract.pdf'),
(45, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 8, NULL, NULL, 'PANEL_CATALOG', 'it-e08-panel_catalog', 'dummytest-v2/enr002/equipment/E08/panel_catalog.pdf'),
(46, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 8, NULL, NULL, 'PCS_SPEC', 'it-e08-pcs_spec', 'dummytest-v2/enr002/equipment/E08/pcs_spec.pdf'),
(47, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 8, NULL, NULL, 'CONSTRUCTION_INVOICE', 'it-e08-construction_invoice', 'dummytest-v2/enr002/equipment/E08/construction_invoice.pdf'),
(48, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 8, NULL, NULL, 'ELECTRICITY_BILL', 'it-e08-electricity_bill', 'dummytest-v2/enr002/equipment/E08/electricity_bill.pdf'),
(49, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 8, NULL, NULL, 'BATTERY_CATALOG', 'it-e08-battery_catalog', 'dummytest-v2/enr002/equipment/E08/battery_catalog.pdf'),
(50, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 8, NULL, NULL, 'SUBSIDY_NOTICE', 'it-e08-subsidy_notice', 'dummytest-v2/enr002/equipment/E08/subsidy_notice.pdf');

-- participating_project
INSERT INTO participating_project (id, is_active, created_at, updated_at, created_by, updated_by, status, project_id, company_id, is_eligible, equipment_id, s3_key_pdf_membership, docusign_envelope_id, signing_status)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'APPROVED', 1, 1, TRUE, 1, 'dummytest-v2/enr002/membership/E01.pdf', NULL, 'NOT_SENT'),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'APPROVED', 1, 1, TRUE, 2, 'dummytest-v2/enr002/membership/E02.pdf', NULL, 'NOT_SENT'),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'APPROVED', 1, 2, TRUE, 3, 'dummytest-v2/enr002/membership/E03.pdf', NULL, 'NOT_SENT'),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'APPROVED', 1, 2, TRUE, 4, 'dummytest-v2/enr002/membership/E04.pdf', NULL, 'NOT_SENT'),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'APPROVED', 1, 3, TRUE, 5, 'dummytest-v2/enr002/membership/E05.pdf', NULL, 'NOT_SENT'),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'OPEN', 1, 4, TRUE, 6, NULL, NULL, NULL),
(7, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'OPEN', 1, 5, TRUE, 7, NULL, NULL, NULL),
(8, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'REJECTED', 1, 6, TRUE, 8, NULL, NULL, NULL);

-- monitoring_data_deletion_history
INSERT INTO monitoring_data_deletion_history (id, is_active, created_at, updated_at, created_by, updated_by, monitoring_record_id, record_time_updated, date_recorded, project_id, site_id, result, failure_reason)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'ITSITE001#ITPROJ001', '2026-08-01T00:00:00Z', '2026-08-01', 1, 1, 'SUCCESS', NULL);

SELECT setval(pg_get_serial_sequence('site_project_relation','id'), (SELECT MAX(id) FROM site_project_relation), true);
SELECT setval(pg_get_serial_sequence('en_r002_equipment','id'), (SELECT MAX(id) FROM en_r002_equipment), true);
SELECT setval(pg_get_serial_sequence('en_r002_equipment_file','id'), (SELECT MAX(id) FROM en_r002_equipment_file), true);
SELECT setval(pg_get_serial_sequence('participating_project','id'), (SELECT MAX(id) FROM participating_project), true);
SELECT setval(pg_get_serial_sequence('monitoring_data_deletion_history','id'), (SELECT MAX(id) FROM monitoring_data_deletion_history), true);
COMMIT;
