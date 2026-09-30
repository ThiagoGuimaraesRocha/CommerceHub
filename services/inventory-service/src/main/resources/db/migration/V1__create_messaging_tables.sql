-- Transactional outbox: messages written in the same transaction as the business change
-- and published to Kafka by the outbox relay (ADR 0006).
CREATE TABLE outbox_events (
    event_id       VARCHAR2(36 CHAR)        PRIMARY KEY,
    aggregate_type VARCHAR2(30 CHAR)        NOT NULL,
    aggregate_id   VARCHAR2(36 CHAR)        NOT NULL,
    event_type     VARCHAR2(60 CHAR)        NOT NULL,
    message_kind   VARCHAR2(10 CHAR)        NOT NULL,
    topic          VARCHAR2(120 CHAR)       NOT NULL,
    message_key    VARCHAR2(36 CHAR)        NOT NULL,
    payload        CLOB                     NOT NULL,
    status         VARCHAR2(20 CHAR)        DEFAULT 'PENDING' NOT NULL,
    attempts       NUMBER(10,0)             DEFAULT 0 NOT NULL,
    last_error     VARCHAR2(1000 CHAR),
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_outbox_kind         CHECK (message_kind IN ('EVENT', 'COMMAND')),
    CONSTRAINT ck_outbox_status       CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_attempts     CHECK (attempts >= 0),
    CONSTRAINT ck_outbox_payload_json CHECK (payload IS JSON),
    CONSTRAINT ck_outbox_published    CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL)
);

CREATE INDEX ix_outbox_status_created ON outbox_events (status, created_at);
CREATE INDEX ix_outbox_aggregate ON outbox_events (aggregate_id);

-- Idempotent consumers: one row per (message, consumer). A duplicate key means "already processed".
CREATE TABLE processed_events (
    event_id      VARCHAR2(36 CHAR)        NOT NULL,
    consumer_name VARCHAR2(100 CHAR)       NOT NULL,
    event_type    VARCHAR2(60 CHAR)        NOT NULL,
    processed_at  TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (event_id, consumer_name)
);
