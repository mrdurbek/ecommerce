CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    payment_ref     VARCHAR(50) NOT NULL UNIQUE,
    order_id        BIGINT NOT NULL,
    order_number    VARCHAR(30) NOT NULL,
    user_id         BIGINT NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(10) NOT NULL DEFAULT 'UZS',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    method          VARCHAR(30) NOT NULL,
    failure_reason  VARCHAR(500),
    refund_reason   VARCHAR(500),
    paid_at         TIMESTAMP,
    refunded_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT

);

CREATE INDEX idx_payments_order_id     ON payments(order_id);
CREATE INDEX idx_payments_order_number ON payments(order_number);
CREATE INDEX idx_payments_user_id      ON payments(user_id);
CREATE INDEX idx_payments_status       ON payments(status);