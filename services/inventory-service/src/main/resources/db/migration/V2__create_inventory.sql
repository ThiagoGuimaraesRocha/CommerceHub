-- Inventory balances and stock reservations (Sprint 4). All-or-nothing reservation means a failed
-- attempt creates no reservation row, so the only statuses in use are RESERVED and RELEASED.
CREATE TABLE inventory_items (
    inventory_item_id VARCHAR2(36 CHAR)        PRIMARY KEY,
    product_id        VARCHAR2(36 CHAR)        NOT NULL,
    available_qty     NUMBER(19,0)             NOT NULL,
    reserved_qty      NUMBER(19,0)             DEFAULT 0 NOT NULL,
    version_no        NUMBER(19,0)             DEFAULT 0 NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_inventory_product UNIQUE (product_id),
    CONSTRAINT ck_inventory_available CHECK (available_qty >= 0),
    CONSTRAINT ck_inventory_reserved CHECK (reserved_qty >= 0)
);

CREATE TABLE stock_reservations (
    reservation_id VARCHAR2(36 CHAR)        PRIMARY KEY,
    order_id       VARCHAR2(36 CHAR)        NOT NULL,
    product_id     VARCHAR2(36 CHAR)        NOT NULL,
    quantity       NUMBER(19,0)             NOT NULL,
    status         VARCHAR2(30 CHAR)        NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_reservation_order_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_reservation_qty CHECK (quantity > 0),
    CONSTRAINT ck_reservation_status CHECK (status IN ('RESERVED', 'RELEASED'))
);

CREATE INDEX ix_reservations_order ON stock_reservations (order_id);
CREATE INDEX ix_reservations_status ON stock_reservations (status);
