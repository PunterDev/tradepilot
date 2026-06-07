-- Optional per-trade journal metadata for trader reflection fields.
-- These columns are edited from TradeDetailModal and are intentionally not part of CSV import.

ALTER TABLE trades
    ADD COLUMN IF NOT EXISTS confidence integer,
    ADD COLUMN IF NOT EXISTS emotion varchar(50);

ALTER TABLE trades
    ALTER COLUMN setup DROP NOT NULL;

ALTER TABLE trades
    ADD COLUMN IF NOT EXISTS notes text;

ALTER TABLE trades DROP CONSTRAINT IF EXISTS chk_trades_confidence;
ALTER TABLE trades
    ADD CONSTRAINT chk_trades_confidence
    CHECK (confidence IS NULL OR confidence BETWEEN 1 AND 5);

ALTER TABLE trades DROP CONSTRAINT IF EXISTS chk_trades_setup;
ALTER TABLE trades
    ADD CONSTRAINT chk_trades_setup
    CHECK (
        setup IS NULL OR setup IN (
            'IFVG',
            'FVG',
            'BREAKER',
            'SMT',
            'ORB',
            'LIQUIDITY_SWEEP',
            'TREND_CONTINUATION',
            'RANGE_REVERSION',
            'OTHER',
            'RECLAIM',
            'COUNTER_TREND'
        )
    );
