CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE companies (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Keep all existing installations usable: their current users and workspace become one legacy company.
INSERT INTO companies (id, name)
SELECT gen_random_uuid(), 'Existing EnterpriseFlow Company'
WHERE EXISTS (SELECT 1 FROM users);

ALTER TABLE users ADD COLUMN company_id UUID REFERENCES companies(id);
ALTER TABLE users ADD COLUMN membership_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
UPDATE users SET company_id = (SELECT id FROM companies WHERE name = 'Existing EnterpriseFlow Company') WHERE company_id IS NULL;
ALTER TABLE users ADD CONSTRAINT chk_users_membership_status CHECK (membership_status IN ('PENDING', 'ACTIVE', 'REJECTED'));
CREATE INDEX idx_users_company ON users(company_id, membership_status);

ALTER TABLE projects ADD COLUMN company_id UUID REFERENCES companies(id);
UPDATE projects p SET company_id = u.company_id FROM users u WHERE p.owner_id = u.id;
CREATE INDEX idx_projects_company ON projects(company_id);

CREATE TABLE join_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_join_request_user_company UNIQUE (user_id, company_id),
    CONSTRAINT chk_join_requests_status CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED'))
);
CREATE INDEX idx_join_requests_company_status ON join_requests(company_id, status);
