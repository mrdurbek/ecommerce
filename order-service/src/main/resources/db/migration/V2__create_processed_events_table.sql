CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

CREATE UNIQUE INDEX idx_processed_events_event_id ON processed_events(event_id);
