CREATE TABLE IF NOT EXISTS securities_master (
    id              SERIAL PRIMARY KEY,
    isin            VARCHAR(12)     NOT NULL,
    symbol          VARCHAR(20),
    name            VARCHAR(255),
    exchange        VARCHAR(5)      NOT NULL,
    series          VARCHAR(10),
    face_value      NUMERIC(10, 2),
    is_active       BOOLEAN         DEFAULT TRUE,
    source_updated_at DATE,
    created_at      TIMESTAMPTZ     DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     DEFAULT NOW(),
    UNIQUE (isin, exchange)
);

CREATE INDEX IF NOT EXISTS idx_securities_isin
    ON securities_master(isin);
CREATE INDEX IF NOT EXISTS idx_securities_symbol
    ON securities_master(symbol, exchange);