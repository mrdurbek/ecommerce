CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(30) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    subtotal        NUMERIC(12, 2) NOT NULL,
    total_amount    NUMERIC(12, 2) NOT NULL,
    notes           TEXT,
    cancelled_reason TEXT,
    confirmed_at    TIMESTAMP,
    shipped_at      TIMESTAMP,
    delivered_at    TIMESTAMP,
    cancelled_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT

);

CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    product_sku     VARCHAR(100),
    qty             NUMERIC(10, 2) NOT NULL,
    unit_price      NUMERIC(10, 2) NOT NULL,
    total_price     NUMERIC(10, 2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

CREATE TABLE shipping_addresses (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    full_name       VARCHAR(200) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    country         VARCHAR(100) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    district        VARCHAR(100),
    street          VARCHAR(100) NOT NULL,
    apartment       VARCHAR(100),
    zip_code        VARCHAR(20),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

CREATE TABLE outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    BIGINT NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    error_message   TEXT,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

-- Indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_outbox_events_status ON outbox_events(status);