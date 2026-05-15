CREATE TABLE inventory_items (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL UNIQUE,
    product_name        VARCHAR(255) NOT NULL,
    sku                 VARCHAR(100) NOT NULL UNIQUE,
    quantity_available  INT NOT NULL DEFAULT 0,
    quantity_reserved   INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 10,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT,
    CONSTRAINT chk_qty_available CHECK (quantity_available >= 0),
    CONSTRAINT chk_qty_reserved  CHECK (quantity_reserved  >= 0)
);

CREATE TABLE stock_movements (
    id              BIGSERIAL PRIMARY KEY,
    inventory_id    BIGINT NOT NULL REFERENCES inventory_items(id),
    product_id      BIGINT NOT NULL,
    movement_type   VARCHAR(30) NOT NULL,
    quantity        INT NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    VARCHAR(100),
    note            VARCHAR(500),
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_inventory_product_id  ON inventory_items(product_id);
CREATE INDEX idx_inventory_sku         ON inventory_items(sku);
CREATE INDEX idx_movements_inventory   ON stock_movements(inventory_id);
CREATE INDEX idx_movements_reference   ON stock_movements(reference_type, reference_id);