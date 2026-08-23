-- Allows imports to preserve uncertainty instead of defaulting unclassified trades to NY_AM.
ALTER TABLE trades
    DROP CONSTRAINT IF EXISTS chk_trades_session;

ALTER TABLE trades
    ADD CONSTRAINT chk_trades_session
    CHECK (session IS NULL OR session IN ('ASIA', 'LONDON', 'NY_AM', 'NY_PM', 'UNKNOWN'));
