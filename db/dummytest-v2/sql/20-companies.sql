-- Static EN-R-002 dummytest-v2 data. Run only on a dedicated test database.
BEGIN;
DO $$ BEGIN IF (SELECT count(*) FROM project WHERE public_id IN ('ITPROJ001','ITPROJ002'))<>2 THEN RAISE EXCEPTION 'Run 10-base.sql first'; END IF; IF EXISTS (SELECT 1 FROM participating_company WHERE company_id='PC000000001') THEN RAISE EXCEPTION '20-companies already applied'; END IF; END $$;
-- participating_company
INSERT INTO participating_company (id, is_active, created_at, updated_at, created_by, updated_by, company_id, name_ja, name_furigana, name_en, email, phone_number, corp_number, postal_code, prefecture, municipality, blocknum, building_name, fax, rep_first_name, rep_first_name_kana, rep_last_name, rep_last_name_kana, rep_phone_number, rep_position, requested_methodologies, activity_information, company_status, email_verified_at, last_rejection_reason, rejected_at)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'PC000000001', '株式会社さくらエナジー', 'サクラエナジー', 'Sakura Energy Co., Ltd.', 'master.c01@example.test', '03-5556-0001', '9100000000001', '100-0005', '東京都', '千代田区丸の内', '１丁目２番３号', '本社ビル', NULL, '陽斗', 'ハルト', '佐藤', 'サトウ', '03-5556-0001', '代表取締役', '["EN-R002"]', '事業所の屋根に自家消費型太陽光発電設備を導入し、電力使用による排出量を削減します。', 'ACTIVE', '2026-08-01 09:00:00', NULL, NULL),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'PC000000002', '株式会社みなと物流', 'ミナトブツリュウ', 'Minato Logistics Co., Ltd.', 'master.c02@example.test', '045-555-0002', '9100000000002', '220-0012', '神奈川県', '横浜市西区みなとみらい', '１丁目２番３号', '本社ビル', NULL, '美月', 'ミヅキ', '高橋', 'タカハシ', '045-555-0002', '代表取締役', '["EN-R002"]', '事業所の屋根に自家消費型太陽光発電設備を導入し、電力使用による排出量を削減します。', 'ACTIVE', '2026-08-01 09:00:00', NULL, NULL),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'PC000000003', '株式会社あおば製作所', 'アオバセイサクショ', 'Aoba Manufacturing Co., Ltd.', 'master.c03@example.test', '022-555-0003', '9100000000003', '980-0021', '宮城県', '仙台市青葉区中央', '１丁目２番３号', '本社ビル', NULL, '蓮', 'レン', '鈴木', 'スズキ', '022-555-0003', '代表取締役', '["EN-R002"]', '事業所の屋根に自家消費型太陽光発電設備を導入し、電力使用による排出量を削減します。', 'ACTIVE', '2026-08-01 09:00:00', NULL, NULL),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'PC000000004', '株式会社北海食品', 'ホッカイショクヒン', 'Hokkai Foods Co., Ltd.', 'master.c04@example.test', '011-555-0004', '9100000000004', '060-0001', '北海道', '札幌市中央区北一条西', '１丁目２番３号', '本社ビル', NULL, '葵', 'アオイ', '田中', 'タナカ', '011-555-0004', '代表取締役', '["EN-R002"]', '事業所の屋根に自家消費型太陽光発電設備を導入し、電力使用による排出量を削減します。', 'ACTIVE', '2026-08-01 09:00:00', NULL, NULL),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'PC000000005', '株式会社東都印刷', 'トウトインサツ', 'Toto Printing Co., Ltd.', 'master.c05@example.test', '03-5556-0005', '9100000000005', '140-0002', '東京都', '品川区東品川', '１丁目２番３号', '本社ビル', NULL, '健一', 'ケンイチ', '伊藤', 'イトウ', '03-5556-0005', '代表取締役', '["EN-R002"]', '事業所の屋根に自家消費型太陽光発電設備を導入し、電力使用による排出量を削減します。', 'ACTIVE', '2026-08-01 09:00:00', NULL, NULL),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'PC000000006', '株式会社信州精密', 'シンシュウセイミツ', 'Shinshu Precision Co., Ltd.', 'master.c06@example.test', '0263-55-0006', '9100000000006', '390-0811', '長野県', '松本市中央', '１丁目２番３号', '本社ビル', NULL, '結衣', 'ユイ', '渡辺', 'ワタナベ', '0263-55-0006', '代表取締役', '["EN-R002"]', '事業所の屋根に自家消費型太陽光発電設備を導入し、電力使用による排出量を削減します。', 'ACTIVE', '2026-08-01 09:00:00', NULL, NULL);

-- site
INSERT INTO site (id, is_active, created_at, updated_at, created_by, updated_by, site_name, postal_code, prefecture, municipality, blocknum, building_name, pic_first_name, pic_first_name_kana, pic_last_name, pic_last_name_kana, pic_email, pic_phone_number, pic_position, participating_company_id, public_id)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'さくらエナジー本社太陽光発電所', '100-0005', '東京都', '千代田区丸の内', '1丁目２番３号', '本社棟', '結衣', 'ユイ', '渡辺', 'ワタナベ', 'user.s01@example.test', '03-5556-1001', '設備管理担当', 1, 'ITSITE001'),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'さくらエナジー物流センター太陽光発電所', '100-0005', '東京都', '千代田区丸の内', '2丁目２番３号', '物流センター棟', '陽斗', 'ハルト', '佐藤', 'サトウ', 'user.s02@example.test', '03-5556-1002', '設備管理担当', 1, 'ITSITE002'),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'みなと物流本社太陽光発電所', '220-0012', '神奈川県', '横浜市西区みなとみらい', '1丁目２番３号', '本社棟', '美月', 'ミヅキ', '高橋', 'タカハシ', 'user.s03@example.test', '045-555-1003', '設備管理担当', 2, 'ITSITE003'),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'みなと物流物流センター太陽光発電所', '220-0012', '神奈川県', '横浜市西区みなとみらい', '2丁目２番３号', '物流センター棟', '蓮', 'レン', '鈴木', 'スズキ', 'user.s04@example.test', '045-555-1004', '設備管理担当', 2, 'ITSITE004'),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'あおば製作所本社太陽光発電所', '980-0021', '宮城県', '仙台市青葉区中央', '1丁目２番３号', '本社棟', '葵', 'アオイ', '田中', 'タナカ', 'user.s05@example.test', '022-555-1005', '設備管理担当', 3, 'ITSITE005'),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '北海食品本社太陽光発電所', '060-0001', '北海道', '札幌市中央区北一条西', '1丁目２番３号', '本社棟', '健一', 'ケンイチ', '伊藤', 'イトウ', 'user.s06@example.test', '011-555-1006', '設備管理担当', 4, 'ITSITE006'),
(7, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '東都印刷本社太陽光発電所', '140-0002', '東京都', '品川区東品川', '1丁目２番３号', '本社棟', '結衣', 'ユイ', '渡辺', 'ワタナベ', 'user.s07@example.test', '03-5556-1007', '設備管理担当', 5, 'ITSITE007'),
(8, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '信州精密本社太陽光発電所', '390-0811', '長野県', '松本市中央', '1丁目２番３号', '本社棟', '陽斗', 'ハルト', '佐藤', 'サトウ', 'user.s08@example.test', '0263-55-1008', '設備管理担当', 6, 'ITSITE008');

-- participating_user
INSERT INTO participating_user (id, is_active, created_at, updated_at, created_by, updated_by, username, first_name, first_name_kana, last_name, last_name_kana, email, phone_number, user_position, password, role, login_attempts, account_locked_until, site_id, company_id, deleted_at, deleted_by, email_otp_login_enabled, auth_version)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'master.c01', '陽斗', 'ハルト', '佐藤', 'サトウ', 'master.c01@example.test', '03-5556-0001', '代表取締役', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'master', 0, NULL, NULL, 1, NULL, NULL, FALSE, 0),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'master.c02', '美月', 'ミヅキ', '高橋', 'タカハシ', 'master.c02@example.test', '045-555-0002', '代表取締役', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'master', 0, NULL, NULL, 2, NULL, NULL, FALSE, 0),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'master.c03', '蓮', 'レン', '鈴木', 'スズキ', 'master.c03@example.test', '022-555-0003', '代表取締役', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'master', 0, NULL, NULL, 3, NULL, NULL, FALSE, 0),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'master.c04', '葵', 'アオイ', '田中', 'タナカ', 'master.c04@example.test', '011-555-0004', '代表取締役', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'master', 0, NULL, NULL, 4, NULL, NULL, FALSE, 0),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'master.c05', '健一', 'ケンイチ', '伊藤', 'イトウ', 'master.c05@example.test', '03-5556-0005', '代表取締役', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'master', 0, NULL, NULL, 5, NULL, NULL, FALSE, 0),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'master.c06', '結衣', 'ユイ', '渡辺', 'ワタナベ', 'master.c06@example.test', '0263-55-0006', '代表取締役', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'master', 0, NULL, NULL, 6, NULL, NULL, FALSE, 0),
(16, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'user.s01', '結衣', 'ユイ', '渡辺', 'ワタナベ', 'user.s01@example.test', '03-5556-1001', '設備管理担当', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'user', 0, NULL, 1, 1, NULL, NULL, FALSE, 0),
(17, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'user.s02', '陽斗', 'ハルト', '佐藤', 'サトウ', 'user.s02@example.test', '03-5556-1002', '設備管理担当', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'user', 0, NULL, 2, 1, NULL, NULL, FALSE, 0),
(18, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'user.s03', '美月', 'ミヅキ', '高橋', 'タカハシ', 'user.s03@example.test', '045-555-1003', '設備管理担当', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'user', 0, NULL, 3, 2, NULL, NULL, FALSE, 0),
(19, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'user.s04', '蓮', 'レン', '鈴木', 'スズキ', 'user.s04@example.test', '045-555-1004', '設備管理担当', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'user', 0, NULL, 4, 2, NULL, NULL, FALSE, 0),
(20, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'user.s05', '葵', 'アオイ', '田中', 'タナカ', 'user.s05@example.test', '022-555-1005', '設備管理担当', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'user', 0, NULL, 5, 3, NULL, NULL, FALSE, 0),
(21, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'user.s06', '健一', 'ケンイチ', '伊藤', 'イトウ', 'user.s06@example.test', '011-555-1006', '設備管理担当', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'user', 0, NULL, 6, 4, NULL, NULL, FALSE, 0),
(22, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'user.s07', '結衣', 'ユイ', '渡辺', 'ワタナベ', 'user.s07@example.test', '03-5556-1007', '設備管理担当', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'user', 0, NULL, 7, 5, NULL, NULL, FALSE, 0),
(23, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', 'user.s08', '陽斗', 'ハルト', '佐藤', 'サトウ', 'user.s08@example.test', '0263-55-1008', '設備管理担当', '$2b$10$wTz09fH3P9iPo1A4O/aqZ..UFEtTUeZ8T0U1fTmKbXHNw/aAoxjyq', 'user', 0, NULL, 8, 6, NULL, NULL, FALSE, 0);

-- notification
INSERT INTO notification (id, is_active, created_at, updated_at, created_by, updated_by, title, message, read, operating_user_id, participating_user_id)
OVERRIDING SYSTEM VALUE
VALUES
(1, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '入会審査のお知らせ', 'テスト企業C01の申請・拠点情報を確認してください。', FALSE, NULL, 1),
(2, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '入会審査のお知らせ', 'テスト企業C02の申請・拠点情報を確認してください。', FALSE, NULL, 2),
(3, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '入会審査のお知らせ', 'テスト企業C03の申請・拠点情報を確認してください。', TRUE, NULL, 3),
(4, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '入会審査のお知らせ', 'テスト企業C04の申請・拠点情報を確認してください。', FALSE, NULL, 4),
(5, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '入会審査のお知らせ', 'テスト企業C05の申請・拠点情報を確認してください。', FALSE, NULL, 5),
(6, TRUE, '2026-08-01 09:00:00', '2026-08-01 09:00:00', 'it.admin', 'it.admin', '入会審査のお知らせ', 'テスト企業C06の申請・拠点情報を確認してください。', TRUE, NULL, 6);

SELECT setval(pg_get_serial_sequence('participating_company','id'), (SELECT MAX(id) FROM participating_company), true);
SELECT setval(pg_get_serial_sequence('site','id'), (SELECT MAX(id) FROM site), true);
SELECT setval(pg_get_serial_sequence('participating_user','id'), (SELECT MAX(id) FROM participating_user), true);
SELECT setval(pg_get_serial_sequence('notification','id'), (SELECT MAX(id) FROM notification), true);
COMMIT;
