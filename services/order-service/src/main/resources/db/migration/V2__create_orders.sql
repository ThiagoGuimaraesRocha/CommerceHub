-- Orders and order items (Sprint 3). Snapshot fields capture product state at purchase time.
CREATE TABLE orders (
    order_id            VARCHAR2(36 CHAR)        PRIMARY KEY,
    customer_id         VARCHAR2(36 CHAR)        NOT NULL,
    status              VARCHAR2(30 CHAR)        NOT NULL,
    total_amount        NUMBER(19,4)             NOT NULL,
    currency_code       VARCHAR2(3 CHAR)         DEFAULT 'BRL' NOT NULL,
    cancellation_reason VARCHAR2(40 CHAR),
    cancellation_note   VARCHAR2(500 CHAR),
    cancelled_by        VARCHAR2(20 CHAR),
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    version_no          NUMBER(19,0)             DEFAULT 0 NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_status CHECK (status IN
        ('CREATED','CONFIRMED','INVENTORY_RESERVED','PAYMENT_PENDING','PAYMENT_APPROVED','COMPLETED','CANCELLED')),
    CONSTRAINT ck_orders_currency CHECK (LENGTH(currency_code) = 3),
    CONSTRAINT ck_orders_cancel_reason CHECK (cancellation_reason IS NULL OR cancellation_reason IN
        ('CHANGED_MIND','ORDERED_BY_MISTAKE','FOUND_BETTER_PRICE','DELIVERY_TIME_TOO_LONG','OTHER',
         'INSUFFICIENT_STOCK','UNKNOWN_PRODUCT','PAYMENT_FAILED')),
    CONSTRAINT ck_orders_cancelled_by CHECK (cancelled_by IS NULL OR cancelled_by IN ('CUSTOMER','SYSTEM')),
    CONSTRAINT ck_orders_cancel_fields CHECK (
        (status = 'CANCELLED' AND cancellation_reason IS NOT NULL AND cancelled_by IS NOT NULL
             AND cancelled_at IS NOT NULL)
        OR (status <> 'CANCELLED' AND cancellation_reason IS NULL AND cancellation_note IS NULL
             AND cancelled_by IS NULL AND cancelled_at IS NULL)),
    CONSTRAINT ck_orders_cancel_note CHECK (cancellation_reason IS NULL OR cancellation_reason <> 'OTHER'
        OR cancellation_note IS NOT NULL)
);

CREATE TABLE order_items (
    order_item_id VARCHAR2(36 CHAR) PRIMARY KEY,
    order_id      VARCHAR2(36 CHAR) NOT NULL,
    product_id    VARCHAR2(36 CHAR) NOT NULL,
    sku           VARCHAR2(64 CHAR) NOT NULL,
    product_name  VARCHAR2(150 CHAR) NOT NULL,
    quantity      NUMBER(19,0)      NOT NULL,
    unit_price    NUMBER(19,4)      NOT NULL,
    line_total    NUMBER(19,4)      NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (order_id),
    CONSTRAINT uk_order_items_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_order_items_qty CHECK (quantity > 0),
    CONSTRAINT ck_order_items_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_total CHECK (line_total >= 0)
);

CREATE INDEX ix_orders_customer ON orders (customer_id);
CREATE INDEX ix_orders_status ON orders (status);
CREATE INDEX ix_order_items_order ON order_items (order_id);
