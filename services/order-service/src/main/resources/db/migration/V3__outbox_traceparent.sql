-- W3C traceparent captured when the outbox row is written so the relay can continue the
-- originating HTTP/saga span on the Kafka record (Sprint 6).
ALTER TABLE outbox_events ADD traceparent VARCHAR2(55 CHAR);
