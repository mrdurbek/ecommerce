
CREATE TABLE order_records (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,
    order_number    VARCHAR(30) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL,
    subtotal        NUMERIC(12, 2) NOT NULL,
    total_amount    NUMERIC(12, 2) NOT NULL,
    item_count      INT NOT NULL DEFAULT 0,

    shipping_city    VARCHAR(100),
    shipping_country VARCHAR(100),

    order_created_at  TIMESTAMP NOT NULL,
    order_confirmed_at TIMESTAMP,
    order_shipped_at   TIMESTAMP,
    order_delivered_at TIMESTAMP,
    order_cancelled_at TIMESTAMP,
    recorded_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_item_records (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    order_number    VARCHAR(30) NOT NULL,
    user_id         BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    product_sku     VARCHAR(100),
    quantity        INT NOT NULL,
    unit_price      NUMERIC(10, 2) NOT NULL,
    total_price     NUMERIC(10, 2) NOT NULL,
    order_created_at TIMESTAMP NOT NULL,
    recorded_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_records (
    id              BIGSERIAL PRIMARY KEY,
    payment_id      BIGINT NOT NULL UNIQUE,
    payment_ref     VARCHAR(50) NOT NULL UNIQUE,
    order_id        BIGINT NOT NULL,
    order_number    VARCHAR(30) NOT NULL,
    user_id         BIGINT NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(10) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    method          VARCHAR(30),
    failure_reason  VARCHAR(500),
    paid_at         TIMESTAMP,
    refunded_at     TIMESTAMP,
    recorded_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE inventory_records (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    order_number    VARCHAR(30) NOT NULL,
    product_id      BIGINT NOT NULL,
    product_sku     VARCHAR(100),
    quantity        INT NOT NULL,
    status          VARCHAR(30) NOT NULL,
    failure_reason  VARCHAR(500),
    event_time      TIMESTAMP NOT NULL,
    recorded_at     TIMESTAMP NOT NULL DEFAULT NOW()
);


CREATE INDEX idx_order_records_user_id        ON order_records(user_id);
CREATE INDEX idx_order_records_status         ON order_records(status);
CREATE INDEX idx_order_records_order_created  ON order_records(order_created_at);
CREATE INDEX idx_order_records_order_number   ON order_records(order_number);

CREATE INDEX idx_order_item_records_order_id  ON order_item_records(order_id);
CREATE INDEX idx_order_item_records_product_id ON order_item_records(product_id);
CREATE INDEX idx_order_item_records_created   ON order_item_records(order_created_at);

CREATE INDEX idx_payment_records_order_id     ON payment_records(order_id);
CREATE INDEX idx_payment_records_user_id      ON payment_records(user_id);
CREATE INDEX idx_payment_records_status       ON payment_records(status);
CREATE INDEX idx_payment_records_paid_at      ON payment_records(paid_at);

CREATE INDEX idx_inventory_records_order_id   ON inventory_records(order_id);
CREATE INDEX idx_inventory_records_product_id ON inventory_records(product_id);
CREATE INDEX idx_inventory_records_status     ON inventory_records(status);