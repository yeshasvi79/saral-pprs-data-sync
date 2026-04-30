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

CREATE TABLE IF NOT EXISTS bse_daily_price (
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(20)     NOT NULL,
    isin            VARCHAR(12),
    name            VARCHAR(255),
    open            NUMERIC(12, 2),
    high            NUMERIC(12, 2),
    low             NUMERIC(12, 2),
    close           NUMERIC(12, 2),
    prev_close      NUMERIC(12, 2),
    volume          BIGINT,
    turnover        NUMERIC(20, 2),
    total_trades    BIGINT,
    trade_date      DATE            NOT NULL,
    created_at      TIMESTAMPTZ     DEFAULT NOW(),
    UNIQUE (code, trade_date)
);

CREATE INDEX IF NOT EXISTS idx_bse_daily_price_isin_date
    ON bse_daily_price(isin, trade_date);

CREATE INDEX IF NOT EXISTS idx_bse_daily_price_date
    ON bse_daily_price(trade_date);    


CREATE TABLE IF NOT EXISTS corporate_action (
    id                  SERIAL PRIMARY KEY,
    symbol              VARCHAR(20),
    isin                VARCHAR(12),
    company_name        VARCHAR(255),
    action_type         VARCHAR(100),
    action_description  TEXT,
    ex_date             DATE,
    record_date         DATE,
    bc_start_date       DATE,
    bc_end_date         DATE,
    nd_start_date       DATE,
    nd_end_date         DATE,
    purpose             TEXT,
    exchange            VARCHAR(5)      DEFAULT 'NSE',
    created_at          TIMESTAMPTZ     DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     DEFAULT NOW(),
    UNIQUE (isin, action_type, ex_date)
);

CREATE INDEX IF NOT EXISTS idx_corporate_action_isin
    ON corporate_action(isin);
CREATE INDEX IF NOT EXISTS idx_corporate_action_ex_date
    ON corporate_action(ex_date);
CREATE INDEX IF NOT EXISTS idx_corporate_action_symbol
    ON corporate_action(symbol);    