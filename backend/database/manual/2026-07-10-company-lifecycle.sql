-- DEV ONLY: destructive schema reset for company lifecycle development.
-- This script intentionally does not migrate legacy status values.
-- Existing company master data is preserved, but every company status is reset to PENDING.

-- ============================================================
-- 1. Drop the lifecycle child table and related indexes
-- ============================================================

DROP TABLE IF EXISTS company_suspension CASCADE;

DROP INDEX IF EXISTS idx_participating_company_status_created;
DROP INDEX IF EXISTS idx_participating_company_subscription_contract;
DROP INDEX IF EXISTS idx_company_suspension_company_created;
DROP INDEX IF EXISTS uq_company_suspension_active_reason;

-- ============================================================
-- 2. Remove legacy lifecycle constraints and columns
-- ============================================================

ALTER TABLE participating_company
    DROP CONSTRAINT IF EXISTS chk_participating_company_status,
    DROP CONSTRAINT IF EXISTS chk_participating_company_subscription_status,
    DROP CONSTRAINT IF EXISTS chk_participating_company_active_status;

ALTER TABLE participating_company
    DROP COLUMN IF EXISTS reg_status,
    DROP COLUMN IF EXISTS company_status,
    DROP COLUMN IF EXISTS subscription_status,
    DROP COLUMN IF EXISTS suspension_reason,
    DROP COLUMN IF EXISTS suspension_date;

-- ============================================================
-- 3. Create the canonical lifecycle and billing columns
-- ============================================================

ALTER TABLE participating_company
    ADD COLUMN company_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN subscription_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- The inherited compatibility flag must follow the canonical company status.
UPDATE participating_company
SET is_active = FALSE;

ALTER TABLE participating_company
    ADD CONSTRAINT chk_participating_company_status
        CHECK (company_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED')),
    ADD CONSTRAINT chk_participating_company_subscription_status
        CHECK (subscription_status IN (
            'ACTIVE',
            'PENDING_RENEWAL',
            'PAST_DUE',
            'EXPIRED',
            'CANCELLED'
        )),
    ADD CONSTRAINT chk_participating_company_active_status
        CHECK (is_active = (company_status = 'ACTIVE'));

-- ============================================================
-- 4. Create the multi-blocker suspension table
-- ============================================================

CREATE TABLE company_suspension (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    reason_code VARCHAR(30) NOT NULL,
    reason_detail VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    resolved_at TIMESTAMP NULL,
    resolved_by VARCHAR(100) NULL,
    resolution_note VARCHAR(500) NULL,

    CONSTRAINT fk_company_suspension_company
        FOREIGN KEY (company_id)
        REFERENCES participating_company(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_company_suspension_reason_code
        CHECK (reason_code IN (
            'BILLING',
            'POLICY_VIOLATION',
            'SECURITY',
            'ADMINISTRATIVE',
            'OTHER'
        ))
);

-- ============================================================
-- 5. Create query and consistency indexes
-- ============================================================

CREATE INDEX idx_participating_company_status_created
    ON participating_company (company_status, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_participating_company_subscription_contract
    ON participating_company (subscription_status, contract_end_date)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_company_suspension_company_created
    ON company_suspension (company_id, created_at DESC, id DESC);

CREATE UNIQUE INDEX uq_company_suspension_active_reason
    ON company_suspension (company_id, reason_code)
    WHERE resolved_at IS NULL;

-- ============================================================
-- 6. Optional dummy-data setup
-- Replace the example IDs with the IDs of the three dummy companies.
-- ============================================================

-- Example: participating company.
-- UPDATE participating_company
-- SET company_status = 'ACTIVE',
--     subscription_status = 'ACTIVE',
--     is_active = TRUE
-- WHERE id = 1;

-- Example: suspended participating company with an administrative blocker.
-- UPDATE participating_company
-- SET company_status = 'SUSPENDED',
--     subscription_status = 'ACTIVE',
--     is_active = FALSE
-- WHERE id = 2;
--
-- INSERT INTO company_suspension (
--     company_id,
--     reason_code,
--     reason_detail,
--     created_by
-- ) VALUES (
--     2,
--     'ADMINISTRATIVE',
--     '開発用の利用停止データです。',
--     'developer'
-- );

-- Example: non-participating pending company.
-- UPDATE participating_company
-- SET company_status = 'PENDING',
--     subscription_status = 'ACTIVE',
--     is_active = FALSE
-- WHERE id = 3;

-- Use this alternative when the third dummy company should be rejected.
-- UPDATE participating_company
-- SET company_status = 'REJECTED',
--     subscription_status = 'ACTIVE',
--     is_active = FALSE
-- WHERE id = 3;

-- ============================================================
-- 7. Verify the development schema and dummy statuses
-- ============================================================

SELECT
    id,
    company_id,
    company_status,
    subscription_status,
    is_active
FROM participating_company
ORDER BY id;

SELECT
    id,
    company_id,
    reason_code,
    reason_detail,
    resolved_at
FROM company_suspension
ORDER BY company_id, created_at DESC;
