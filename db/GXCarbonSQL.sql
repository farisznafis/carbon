-- ============================
-- DROP TABLES (dependency-safe)
-- ============================

DROP TABLE IF EXISTS site_project_relation CASCADE;
DROP TABLE IF EXISTS project CASCADE;
DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS operating_user CASCADE;
DROP TABLE IF EXISTS participating_user CASCADE;
DROP TABLE IF EXISTS site CASCADE;
DROP TABLE IF EXISTS participating_company CASCADE;
DROP TABLE IF EXISTS operator_company CASCADE;
DROP TABLE IF EXISTS project_solar_data CASCADE;
DROP TABLE IF EXISTS project_files CASCADE;
DROP TABLE IF EXISTS EN_R002_reduction_result_total CASCADE;
DROP TABLE IF EXISTS EN_R002_reduction_result CASCADE;
DROP TABLE IF EXISTS batch_processing_history CASCADE;
DROP TABLE IF EXISTS record_cancel_request CASCADE;
DROP TABLE IF EXISTS site_r002_member_data CASCADE;
DROP TABLE IF EXISTS site_r002_equipment_file  CASCADE;
DROP TABLE IF EXISTS company_deletion_request CASCADE;
DROP TABLE IF EXISTS subscription_log CASCADE;
DROP TABLE IF EXISTS notification CASCADE;


-- ============================
-- CREATE TABLES
-- ============================

-- Operator Company
CREATE TABLE operator_company (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	is_active BOOLEAN NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    company_id VARCHAR(12) NOT NULL UNIQUE,
    name_ja VARCHAR(255) NOT NULL,
    name_furigana VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    corp_number VARCHAR(13) NOT NULL UNIQUE CHECK (corp_number ~ '^[0-9]{13}$'),
    postal_code VARCHAR(8) NOT NULL CHECK (postal_code ~ '^[0-9]{3}-?[0-9]{4}$'),
    prefecture VARCHAR(10) NOT NULL,
    municipality VARCHAR(150) NOT NULL,
    blocknum VARCHAR(100) NOT NULL,
    building_name VARCHAR(255),
    rep_first_name VARCHAR(50) NOT NULL,
    rep_first_name_kana VARCHAR(50) NOT NULL,
    rep_last_name VARCHAR(50) NOT NULL,
    rep_last_name_kana VARCHAR(50) NOT NULL,
    rep_email VARCHAR(254) NOT NULL,
    rep_phone_number VARCHAR(20) NOT NULL,
    rep_position VARCHAR(100) NOT NULL
);

-- Account
CREATE TABLE account (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	is_active BOOLEAN NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    acct_no VARCHAR(20) NOT NULL UNIQUE,
    holder_account_name_kana VARCHAR(50) NOT NULL,
    holder_account_name VARCHAR(50) NOT NULL,
    holder_corp_number VARCHAR(13) UNIQUE CHECK (holder_corp_number ~ '^[0-9]{13}$'),
    holder_prefecture VARCHAR(100) NOT NULL,
    holder_city VARCHAR(100) NOT NULL,
    holder_address VARCHAR(500) NOT NULL,
    is_holder_undecided BOOLEAN NOT NULL, 
    operator_company_id INT NOT NULL REFERENCES operator_company(id)
);

-- Project
CREATE TABLE project (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	is_active BOOLEAN NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    project_name VARCHAR(100) NOT NULL,
    project_number VARCHAR(10) NOT NULL UNIQUE CHECK (project_number ~ '^P[0-9]+$'),
    project_status VARCHAR(10) NOT NULL,
    rep_first_name VARCHAR(50) NOT NULL,
    rep_first_name_kana VARCHAR(50) NOT NULL,
    rep_last_name VARCHAR(50) NOT NULL,
    rep_last_name_kana VARCHAR(50) NOT NULL,
    rep_email VARCHAR(254) NOT NULL,
    rep_phone_number VARCHAR(20) NOT NULL,
    rep_position VARCHAR(100) NOT NULL,
    pic_first_name VARCHAR(50) NOT NULL,
    pic_first_name_kana VARCHAR(50) NOT NULL,
    pic_last_name VARCHAR(50) NOT NULL,
    pic_last_name_kana VARCHAR(50) NOT NULL,
    pic_email VARCHAR(254) NOT NULL,
    pic_phone_number VARCHAR(20) NOT NULL,
    pic_position VARCHAR(100) NOT NULL,
    data_management_pic VARCHAR(50) NOT NULL,
    emission_reduction DECIMAL(12,1),
    baseline_emission DECIMAL(12,1),
    post_project_emission DECIMAL(12,1),
    regional_category VARCHAR(10) NOT NULL CHECK (regional_category IN ('北海道','東北','関東','中部','近畿','中国','四国','九州・沖縄','全国')),
    new_members_total INT NOT NULL,
    withdrawn_members INT NOT NULL,
    cumulative_members_total INT NOT NULL,
    members_total INT,
    methodology_version VARCHAR(10) NOT NULL CHECK (methodology_version ~ '^Ver\.[0-9]+(\.[0-9]+)?$'),
    method_category VARCHAR(10) NOT NULL CHECK (method_category IN ('EN-S','EN-R','IN','AG','WA')),
    method_number VARCHAR(3) NOT NULL UNIQUE CHECK (method_number ~ '^[0-9]{3}$'),
    method_name VARCHAR(255) NOT NULL,
    method_main_emission VARCHAR(255) NOT NULL,
    method_combi_feasibility BOOLEAN NOT NULL,
    method_details VARCHAR(1000) NOT NULL,
    operator_company_id INT NOT NULL REFERENCES operator_company(id)
);

-- Participating_company
CREATE TABLE participating_company (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- Tambah DEFAULT TRUE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    company_id VARCHAR(12) NOT NULL UNIQUE,
    name_ja VARCHAR(255) NOT NULL,
    name_furigana VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    corp_number VARCHAR(13) NOT NULL UNIQUE CHECK (corp_number ~ '^[0-9]{13}$'),
    postal_code VARCHAR(8) NOT NULL CHECK (postal_code ~ '^[0-9]{3}-?[0-9]{4}$'),
    prefecture VARCHAR(10) NOT NULL,
    municipality VARCHAR(150) NOT NULL,
    blocknum VARCHAR(100) NOT NULL,
    building_name VARCHAR(255),
    fax VARCHAR(20),
    rep_first_name VARCHAR(50) NOT NULL,
    rep_first_name_kana VARCHAR(50) NOT NULL,
    rep_last_name VARCHAR(50) NOT NULL,
    rep_last_name_kana VARCHAR(50) NOT NULL,
    rep_email VARCHAR(254) NOT NULL,
    rep_phone_number VARCHAR(20) NOT NULL,
    rep_position VARCHAR(100) NOT NULL,
    contract_end_date DATE,
    payment_method VARCHAR(50),
    last_renewal_date DATE,
    suspension_reason TEXT,
    suspension_date DATE,
    subscription_status VARCHAR(20),
    reg_status VARCHAR(20),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Site
CREATE TABLE site (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	is_active BOOLEAN NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    site_name VARCHAR(255) NOT NULL,
    postal_code VARCHAR(8) NOT NULL CHECK (postal_code ~ '^[0-9]{3}-?[0-9]{4}$'),
    prefecture VARCHAR(10) NOT NULL,
    municipality VARCHAR(150) NOT NULL,
    blocknum VARCHAR(100) NOT NULL,
    building_name VARCHAR(255),
    pic_first_name VARCHAR(50) NOT NULL,
    pic_first_name_kana VARCHAR(50) NOT NULL,
    pic_last_name VARCHAR(50) NOT NULL,
    pic_last_name_kana VARCHAR(50) NOT NULL,
    pic_email VARCHAR(254) NOT NULL,
    pic_phone_number VARCHAR(20) NOT NULL,
    pic_position VARCHAR(100) NOT NULL,
    participating_company_id INT NOT NULL REFERENCES participating_company(id) ON DELETE CASCADE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Site-Project Relation
CREATE TABLE site_project_relation (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	is_active BOOLEAN NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    project_id INT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    site_id INT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    application_date DATE NOT NULL,
    activity_start_date DATE NOT NULL,
    participating_company_id INT NOT NULL REFERENCES participating_company(id) ON DELETE CASCADE,
    status VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Project Files
CREATE TABLE project_files (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    project_id INT NOT NULL UNIQUE REFERENCES project(id) ON UPDATE CASCADE ON DELETE CASCADE,
    certificate_registration TEXT,
    report_validation TEXT,
    report_verification TEXT
);

-- Operating User
CREATE TABLE operating_user (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	is_active BOOLEAN NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    first_name_kana VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    last_name_kana VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('operator', 'user')),
	login_attempts INT NOT NULL,
	account_locked_until TIMESTAMP,
    company_id INT NOT NULL
        REFERENCES operator_company(id)
);


-- site_r002_member_data
CREATE TABLE site_r002_member_data (
    id SERIAL PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    project_id INT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    site_id INT NOT NULL REFERENCES site(id) ON DELETE CASCADE,
    participating_company_id INT NOT NULL REFERENCES participating_company(id) ON DELETE CASCADE,
    methodology_number CHAR(3) NOT NULL,
    is_eligible BOOLEAN NOT NULL,
    equipment_op_date DATE NOT NULL,
    nominal_max_output DECIMAL(10,1) NOT NULL,
    pcs_manufacturer VARCHAR(100) NOT NULL,
    pcs_model VARCHAR(50) NOT NULL,
    power_cond_unit_count INT NOT NULL,
    device_no VARCHAR(100) NOT NULL,
    power_cond_standby DECIMAL(8,2) NOT NULL,
    has_battery BOOLEAN NOT NULL,
    battery_maker VARCHAR(100),
    battery_model VARCHAR(100),
    battery_capacity DECIMAL(10,1),
    ict_equipment_power_consumption DECIMAL(12,3) NOT NULL,
    additional_equipment BOOLEAN,
    additional_equipment_date DATE,
    equipment_type VARCHAR(50),
    unit_count INT,
    device_unique_no VARCHAR(100),
    total_investment INT,
    has_subsidy BOOLEAN NOT NULL,
    subsidy_name VARCHAR(200),
    subsidy_amount DECIMAL(15,0),
    pre_project_unit_price DECIMAL(10,2),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- site_r002_equipment_file 
CREATE TABLE site_r002_equipment_file (
    id SERIAL PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    member_data_id INT NOT NULL,
    file_id VARCHAR(100), 
    file_type VARCHAR(50) NOT NULL,
    s3_key VARCHAR(1000) NOT NULL, 
    original_name VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    CONSTRAINT fk_member_data 
        FOREIGN KEY (member_data_id) 
        REFERENCES site_r002_member_data(id) 
        ON DELETE CASCADE
);

-- Participating User
CREATE TABLE participating_user (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	is_active BOOLEAN NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    first_name_kana VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    last_name_kana VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
	user_position VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('master', 'user')),
	login_attempts INT NOT NULL,
	account_locked_until TIMESTAMP,
    site_id INT REFERENCES site(id) ON DELETE SET NULL,
    company_id INT NOT NULL
        REFERENCES participating_company(id),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);


CREATE TABLE EN_R002_reduction_result_total(
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    batch_id INT,
    processing_date DATE,
    project_id INT REFERENCES project(id) ON DELETE CASCADE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    empj DECIMAL(12,3),
    empj_m DECIMAL(12,3),
    empj_s DECIMAL(12,3),
    empj_s_control DECIMAL(12,3),
    empj_s_battery DECIMAL(12,3),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

CREATE TABLE EN_R002_reduction_result(
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    batch_id INT,
    processing_date DATE,
    project_id INT REFERENCES project(id) ON DELETE CASCADE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    site_id INT REFERENCES site(id) ON DELETE CASCADE,
    empj DECIMAL(12,3),
    empj_m DECIMAL(12,3),
    empj_s DECIMAL(12,3),
    empj_s_control DECIMAL(12,3),
    empj_s_battery DECIMAL(12,3),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

CREATE TABLE batch_process_history(
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(10),
    claimed_by VARCHAR(50),
    claimed_at TIMESTAMP
);

CREATE TABLE record_cancel_request(
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    record_pk VARCHAR(10) NOT NULL,
    record_sk VARCHAR(50) NOT NULL,
    status VARCHAR(50),
    requested_by VARCHAR(100),
    requested_at TIMESTAMP,
    reason TEXT,
    decided_by VARCHAR(100),
    decided_at TIMESTAMP,
    offset_record_pk VARCHAR(10),
    offset_record_sk VARCHAR(50),
    site_id INT NOT NULL REFERENCES site(id) ON UPDATE CASCADE ON DELETE CASCADE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- subscription_log
CREATE TABLE  subscription_log (
    id SERIAL PRIMARY KEY,

    company_id INTEGER REFERENCES participating_company(id),

    action_type VARCHAR(50) NOT NULL,
    action_date TIMESTAMP NOT NULL,

    amount NUMERIC(10,2),
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100),

    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),

    previous_end_date DATE,
    new_end_date DATE,

    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE notification (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,

    operating_user_id INT REFERENCES operating_user(id) ON UPDATE CASCADE ON DELETE CASCADE,
    participating_user_id INT REFERENCES participating_user(id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Company Deletion Request
CREATE TABLE company_deletion_request (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    company_id INTEGER REFERENCES participating_company(id),
    requested_by VARCHAR(100) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by VARCHAR(100),
    decided_at TIMESTAMP,
    rejection_reason TEXT
);

-- 読み込む負担を軽減するために、頻繁に検索されるカラムにインデックスを作成します。
--テーブル数検索で制限整理のために、頻繁に検索されるカラムにインデックスを作成します。
CREATE INDEX idx_participating_user_company_id ON participating_user (company_id);

CREATE INDEX idx_participating_user_site_id ON participating_user (site_id);

CREATE INDEX idx_site_participating_company_id ON site (participating_company_id);
