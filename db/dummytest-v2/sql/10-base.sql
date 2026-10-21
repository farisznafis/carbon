-- Static EN-R-002 dummytest-v2 data. Run only on a dedicated test database.
BEGIN;
DO $$ BEGIN IF EXISTS (SELECT 1 FROM operator_company WHERE company_id='OP000000001') THEN RAISE EXCEPTION '10-base already applied'; END IF; END $$;
-- operator_company
INSERT INTO operator_company (id, is_active, created_at, updated_at, created_by, updated_by, company_id, name_ja, name_furigana, name_en, email, phone_number, corp_number, postal_code, prefecture, municipality, blocknum, building_name, rep_first_name, rep_first_name_kana, rep_last_name, rep_last_name_kana, rep_email, rep_phone_number, rep_position)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'OP000000001', '株式会社創成カーボン', 'ソウセイカーボン', 'Sosei Carbon Co., Ltd.', 'operator-company@example.test', '03-5555-0100', '9000000000001', '105-0001', '東京都', '港区虎ノ門', '１丁目２番３号', '虎ノ門テストビル', '真耶', 'マヤ', '増田', 'マスダ', 'admin@example.test', '03-5555-0101', '代表取締役');

-- operating_user
INSERT INTO operating_user (id, is_active, created_at, updated_at, created_by, updated_by, username, first_name, first_name_kana, last_name, last_name_kana, email, phone_number, password, role, login_attempts, account_locked_until, company_id, email_otp_login_enabled, auth_version)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'it.admin', '真耶', 'マヤ', '増田', 'マスダ', 'admin@example.test', '03-5555-0101', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'admin', 0, NULL, 1, FALSE, 0),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'it.operator', '沙奈', 'サナ', '鳥居', 'トリイ', 'operator@example.test', '03-5555-0102', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'operator', 0, NULL, 1, FALSE, 0);

-- account
INSERT INTO account (id, is_active, created_at, updated_at, created_by, updated_by, acct_no, holder_account_name_kana, holder_account_name, holder_corp_number, holder_prefecture, holder_city, holder_address, is_holder_undecided, operator_company_id)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '9000000001', 'ソウセイカーボン', '株式会社創成カーボン', '9000000000001', '東京都', '港区', '虎ノ門１丁目２番３号', FALSE, 1);

-- project
INSERT INTO project (id, is_active, created_at, updated_at, created_by, updated_by, project_name, project_number, project_status, rep_first_name, rep_first_name_kana, rep_last_name, rep_last_name_kana, rep_email, rep_phone_number, rep_position, pic_first_name, pic_first_name_kana, pic_last_name, pic_last_name_kana, pic_email, pic_phone_number, pic_position, data_management_pic, emission_reduction, baseline_emission, post_project_emission, regional_category, new_members_total, withdrawn_members, cumulative_members_total, members_total, methodology_version, method_category, method_number, method_name, method_main_emission, method_combi_feasibility, method_details, use_purpose, operator_company_id, public_id, status_finalized_at, project_started_at)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '地域太陽光発電プログラム第一期', 'P000001', 'ACTIVE', '真耶', 'マヤ', '増田', 'マスダ', 'admin@example.test', '03-5555-0101', '代表取締役', '沙奈', 'サナ', '鳥居', 'トリイ', 'operator@example.test', '03-5555-0102', 'プロジェクト管理担当', '鳥居 沙奈', 0, 0, 0, '全国', 5, 0, 5, 5, 'Ver.3.7', 'EN-R', '002', '太陽光発電設備の導入', '電力使用に伴う二酸化炭素排出量', TRUE, '自家消費型太陽光発電設備による排出削減活動。', '地域事業所の排出削減とクレジット創出。', 1, 'ITPROJ001', '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '地域太陽光発電プログラム第二期', 'P000002', 'ACTIVE', '真耶', 'マヤ', '増田', 'マスダ', 'admin@example.test', '03-5555-0101', '代表取締役', '沙奈', 'サナ', '鳥居', 'トリイ', 'operator@example.test', '03-5555-0102', 'プロジェクト管理担当', '鳥居 沙奈', 0, 0, 0, '全国', 0, 0, 0, 0, 'Ver.3.7', 'EN-R', '002', '太陽光発電設備の導入', '電力使用に伴う二酸化炭素排出量', TRUE, '自家消費型太陽光発電設備による排出削減活動。', '地域事業所の排出削減とクレジット創出。', 1, 'ITPROJ002', '2026-08-01 09:00:00', '2026-08-01 09:00:00');

-- project_files
INSERT INTO project_files (id, is_active, created_at, updated_at, created_by, updated_by, project_id, deleted_at, deleted_by, file_type, s3_key, original_file_name, owned_by, file_version)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'PROJECT_PLAN', 'dummytest-v2/enr002/projects/P01-plan.xlsx', 'P01-plan.xlsx', TRUE, 1),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'REGISTRATION_RESULT', 'dummytest-v2/enr002/projects/P01-registration_result.pdf', 'P01-registration_result.pdf', FALSE, 1),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 1, NULL, NULL, 'VALIDATION_REPORT', 'dummytest-v2/enr002/projects/P01-validation_report.pdf', 'P01-validation_report.pdf', FALSE, 1),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'PROJECT_PLAN', 'dummytest-v2/enr002/projects/P02-plan.xlsx', 'P02-plan.xlsx', TRUE, 1),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'REGISTRATION_RESULT', 'dummytest-v2/enr002/projects/P02-registration_result.pdf', 'P02-registration_result.pdf', FALSE, 1),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 2, NULL, NULL, 'VALIDATION_REPORT', 'dummytest-v2/enr002/projects/P02-validation_report.pdf', 'P02-validation_report.pdf', FALSE, 1);

DO $$ BEGIN IF (SELECT count(*) FROM permission WHERE code IN ('PROJECT_CREATE','PROJECT_READ','PROJECT_UPDATE','PROJECT_STATUS_UPDATE','PROJECT_APPLICATION_OUTPUT','PARTICIPATING_COMPANY_READ','PARTICIPATING_COMPANY_APPLICATION_REVIEW','PARTICIPATING_COMPANY_SUSPEND','MASTER_USER_PASSWORD_RESET','MONITORING_READ','MONITORING_DELETE','MONITORING_CANCEL','MONITORING_BATCH_HISTORY_READ','MONITORING_BATCH_ERROR_READ','OPERATOR_USER_READ','METHODOLOGY_READ','APPLICATION_FORM_READ')) <> 17 THEN RAISE EXCEPTION 'Permission catalog incomplete'; END IF; END $$;
INSERT INTO operating_user_permission(operating_user_id,permission_id,created_by) SELECT 2,id,'it.admin' FROM permission WHERE code IN ('PROJECT_CREATE','PROJECT_READ','PROJECT_UPDATE','PROJECT_STATUS_UPDATE','PROJECT_APPLICATION_OUTPUT','PARTICIPATING_COMPANY_READ','PARTICIPATING_COMPANY_APPLICATION_REVIEW','PARTICIPATING_COMPANY_SUSPEND','MASTER_USER_PASSWORD_RESET','MONITORING_READ','MONITORING_DELETE','MONITORING_CANCEL','MONITORING_BATCH_HISTORY_READ','MONITORING_BATCH_ERROR_READ','OPERATOR_USER_READ','METHODOLOGY_READ','APPLICATION_FORM_READ');
SELECT setval(pg_get_serial_sequence('operator_company','id'), (SELECT MAX(id) FROM operator_company), true);
SELECT setval(pg_get_serial_sequence('operating_user','id'), (SELECT MAX(id) FROM operating_user), true);
SELECT setval(pg_get_serial_sequence('account','id'), (SELECT MAX(id) FROM account), true);
SELECT setval(pg_get_serial_sequence('project','id'), (SELECT MAX(id) FROM project), true);
SELECT setval(pg_get_serial_sequence('project_files','id'), (SELECT MAX(id) FROM project_files), true);
COMMIT;
