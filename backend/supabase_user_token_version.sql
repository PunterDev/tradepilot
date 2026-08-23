-- TradePilot JWT session invalidation support.
-- Safe to run in Supabase PostgreSQL before deploying code that validates users.token_version.

ALTER TABLE users
ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;

UPDATE users
SET token_version = 0
WHERE token_version IS NULL;
