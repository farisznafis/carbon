-- MANUAL MODE ONLY. Do not run before the sequential seed.
-- Adds only operator company + admin because the frontend cannot bootstrap them.
BEGIN;
DO $$ BEGIN IF EXISTS (SELECT 1 FROM operator_company) THEN RAISE EXCEPTION 'Bootstrap requires an empty operator domain'; END IF; END $$;
-- operator_company: 1 rows
INSERT INTO operator_company (id, is_active, created_at, updated_at, created_by, updated_by, company_id, name_ja, name_furigana, name_en, email, phone_number, corp_number, postal_code, prefecture, municipality, blocknum, building_name, rep_first_name, rep_first_name_kana, rep_last_name, rep_last_name_kana, rep_email, rep_phone_number, rep_position)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'OP000000001', '株式会社創成カーボン', 'ソウセイカーボン', 'Sosei Carbon Co., Ltd.', 'operator-company@example.test', '03-5555-0100', '9000000000001', '105-0001', '東京都', '港区虎ノ門', '１丁目２番３号', '虎ノ門テストビル', '真耶', 'マヤ', '増田', 'マスダ', 'admin@example.test', '03-5555-0101', '代表取締役');

-- operating_user: 1 rows
INSERT INTO operating_user (id, is_active, created_at, updated_at, created_by, updated_by, username, first_name, first_name_kana, last_name, last_name_kana, email, phone_number, password, role, login_attempts, account_locked_until, company_id, email_otp_login_enabled, auth_version)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'it.admin', '真耶', 'マヤ', '増田', 'マスダ', 'admin@example.test', '03-5555-0101', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'admin', 0, NULL, 1, FALSE, 0);

SELECT setval(pg_get_serial_sequence('operator_company','id'), (SELECT MAX(id) FROM operator_company), true);
SELECT setval(pg_get_serial_sequence('operating_user','id'), (SELECT MAX(id) FROM operating_user), true);
COMMIT;
