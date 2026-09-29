CREATE TABLE products (
    product_id    VARCHAR2(36 CHAR)        PRIMARY KEY,
    sku           VARCHAR2(64 CHAR)        NOT NULL,
    name          VARCHAR2(150 CHAR)       NOT NULL,
    description   VARCHAR2(1000 CHAR),
    category_code VARCHAR2(60 CHAR)        NOT NULL,
    price         NUMBER(19,4)             NOT NULL,
    currency_code VARCHAR2(3 CHAR)         DEFAULT 'BRL' NOT NULL,
    active        NUMBER(1)                DEFAULT 1 NOT NULL,
    version_no    NUMBER(19,0)             DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_products_sku      UNIQUE (sku),
    CONSTRAINT ck_products_price    CHECK (price >= 0),
    CONSTRAINT ck_products_active   CHECK (active IN (0, 1)),
    CONSTRAINT ck_products_currency CHECK (LENGTH(currency_code) = 3)
);

CREATE INDEX ix_products_category ON products (category_code);
CREATE INDEX ix_products_active ON products (active);
