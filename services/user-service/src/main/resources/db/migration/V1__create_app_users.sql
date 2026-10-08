CREATE TABLE app_users (
    user_id       VARCHAR2(36 CHAR)        PRIMARY KEY,
    email         VARCHAR2(320 CHAR)       NOT NULL,
    password_hash VARCHAR2(255 CHAR)       NOT NULL,
    full_name     VARCHAR2(150 CHAR)       NOT NULL,
    role_code     VARCHAR2(30 CHAR)        NOT NULL,
    active        NUMBER(1)                DEFAULT 1 NOT NULL,
    version_no    NUMBER(19,0)             DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_active CHECK (active IN (0, 1)),
    CONSTRAINT ck_app_users_role CHECK (role_code IN ('CUSTOMER', 'ADMIN'))
);
