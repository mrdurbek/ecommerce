CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    phone_number            VARCHAR(20),
    is_enabled              BOOLEAN NOT NULL DEFAULT FALSE,
    is_account_non_expired  BOOLEAN NOT NULL DEFAULT TRUE,
    is_account_non_locked   BOOLEAN NOT NULL DEFAULT TRUE,
    is_credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts   INT NOT NULL DEFAULT 0,
    locked_until            TIMESTAMP,
    last_login_at           TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address  VARCHAR(50),
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE email_verifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

-- Permissions table
CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    module      VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_email_verifications_token ON email_verifications(token);

CREATE INDEX idx_role_permissions_role_id       ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Default roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Standard user with basic access'),
    ('ROLE_ADMIN', 'Administrator with full access'),
    ('ROLE_MODERATOR', 'Moderator with limited admin access');

-- Default permissions
INSERT INTO permissions (name, description, module) VALUES
    -- Product
    ('PRODUCT_VIEW',    'View products',          'PRODUCT'),
    ('PRODUCT_CREATE',  'Create products',         'PRODUCT'),
    ('PRODUCT_UPDATE',  'Update products',         'PRODUCT'),
    ('PRODUCT_DELETE',  'Delete products',         'PRODUCT'),
    -- Order
    ('ORDER_VIEW',      'View own orders',         'ORDER'),
    ('ORDER_VIEW_ALL',  'View all orders',         'ORDER'),
    ('ORDER_UPDATE',    'Update order status',     'ORDER'),
    ('ORDER_CANCEL',    'Cancel orders',           'ORDER'),
    -- Inventory
    ('INVENTORY_VIEW',  'View inventory',          'INVENTORY'),
    ('INVENTORY_MANAGE','Manage inventory stock',  'INVENTORY'),
    -- User
    ('USER_VIEW',       'View users',              'USER'),
    ('USER_MANAGE',     'Manage users',            'USER'),
    -- Report
    ('REPORT_VIEW',     'View reports',            'REPORT');

-- Assign all permissions to ROLE_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN';

-- Assign basic permissions to ROLE_USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER'
  AND p.name IN ('PRODUCT_VIEW', 'ORDER_VIEW', 'ORDER_CANCEL');

-- Assign moderator permissions to ROLE_MODERATOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_MODERATOR'
  AND p.name IN ('PRODUCT_VIEW', 'PRODUCT_CREATE', 'PRODUCT_UPDATE',
                 'ORDER_VIEW', 'ORDER_VIEW_ALL', 'ORDER_UPDATE', 'ORDER_CANCEL',
                 'INVENTORY_VIEW', 'REPORT_VIEW');

-- Default admin user
-- password = admin123
INSERT INTO users (email, password, first_name, last_name, is_enabled, is_account_non_expired, is_account_non_locked, is_credentials_non_expired) 
VALUES ('admin@example.com', '$2b$12$Jl4GWMjoMZPXKGYcd1Q3u.7Xw9j5REqqoDgP3JX/Gx2lGq5JJaXHS', 'Admin', 'User', true, true, true, true);

-- Assign ROLE_ADMIN to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@example.com' AND r.name = 'ROLE_ADMIN';
