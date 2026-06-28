-- TradePilot additive schema consistency fixes.
-- Safe to run repeatedly in Supabase/Postgres. This file fills gaps for
-- environments that predate the feature-specific migrations.

CREATE TABLE IF NOT EXISTS users (
    id bigserial PRIMARY KEY,
    name varchar(100) NOT NULL,
    email varchar(150) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    email_verified boolean NOT NULL DEFAULT false,
    plan varchar(20) NOT NULL DEFAULT 'FREE',
    role varchar(20) NOT NULL DEFAULT 'USER',
    subscription_status varchar(20) NOT NULL DEFAULT 'NONE',
    current_period_start timestamp,
    current_period_end timestamp,
    stripe_customer_id varchar(255),
    stripe_subscription_id varchar(255),
    stripe_cancel_at_period_end boolean NOT NULL DEFAULT false,
    stripe_cancel_at timestamp,
    stripe_canceled_at timestamp,
    pro_until timestamp,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS name varchar(100),
    ADD COLUMN IF NOT EXISTS email varchar(150),
    ADD COLUMN IF NOT EXISTS password_hash varchar(255),
    ADD COLUMN IF NOT EXISTS email_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS plan varchar(20) NOT NULL DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS role varchar(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS subscription_status varchar(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS current_period_start timestamp,
    ADD COLUMN IF NOT EXISTS current_period_end timestamp,
    ADD COLUMN IF NOT EXISTS stripe_customer_id varchar(255),
    ADD COLUMN IF NOT EXISTS stripe_subscription_id varchar(255),
    ADD COLUMN IF NOT EXISTS stripe_cancel_at_period_end boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS stripe_cancel_at timestamp,
    ADD COLUMN IF NOT EXISTS stripe_canceled_at timestamp,
    ADD COLUMN IF NOT EXISTS pro_until timestamp,
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS trading_accounts (
    id bigserial PRIMARY KEY,
    user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    prop_firm varchar(255) NOT NULL,
    account_number varchar(255),
    account_type varchar(255),
    starting_balance numeric(19, 2),
    default_risk_amount numeric(18, 2),
    status varchar(255) NOT NULL,
    created_at timestamp NOT NULL DEFAULT now()
);

ALTER TABLE trading_accounts
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS name varchar(255),
    ADD COLUMN IF NOT EXISTS prop_firm varchar(255),
    ADD COLUMN IF NOT EXISTS account_number varchar(255),
    ADD COLUMN IF NOT EXISTS account_type varchar(255),
    ADD COLUMN IF NOT EXISTS starting_balance numeric(19, 2),
    ADD COLUMN IF NOT EXISTS default_risk_amount numeric(18, 2),
    ADD COLUMN IF NOT EXISTS status varchar(255),
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS import_batches (
    id bigserial PRIMARY KEY,
    user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    account_id bigint REFERENCES trading_accounts(id) ON DELETE SET NULL,
    file_name varchar(255),
    total_rows integer,
    imported_rows integer,
    skipped_duplicates integer,
    imported_at timestamp
);

ALTER TABLE import_batches
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS account_id bigint REFERENCES trading_accounts(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS file_name varchar(255),
    ADD COLUMN IF NOT EXISTS total_rows integer,
    ADD COLUMN IF NOT EXISTS imported_rows integer,
    ADD COLUMN IF NOT EXISTS skipped_duplicates integer,
    ADD COLUMN IF NOT EXISTS imported_at timestamp;

CREATE TABLE IF NOT EXISTS trades (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    symbol varchar(20) NOT NULL,
    market varchar(20) NOT NULL,
    trade_date date NOT NULL,
    entry_time timestamp,
    exit_time timestamp,
    side varchar(20) NOT NULL,
    setup varchar(50),
    session varchar(20) NOT NULL,
    entry_price numeric(18, 6),
    stop_price numeric(18, 6),
    target_price numeric(18, 6),
    exit_price numeric(18, 6),
    quantity integer,
    risk_amount numeric(18, 2),
    pnl_amount numeric(18, 2),
    pnl_r numeric(10, 2),
    outcome varchar(20) NOT NULL,
    confidence integer,
    confidence_rating integer,
    emotion varchar(50),
    execution_rating integer,
    followed_plan boolean,
    mistake_tag varchar(50),
    notes text,
    psychology_notes text,
    lesson_learned text,
    screenshot_url varchar(500),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    account_id bigint REFERENCES trading_accounts(id) ON DELETE SET NULL,
    import_batch_id bigint REFERENCES import_batches(id) ON DELETE SET NULL,
    external_trade_key varchar(255)
);

ALTER TABLE trades
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS symbol varchar(20),
    ADD COLUMN IF NOT EXISTS market varchar(20),
    ADD COLUMN IF NOT EXISTS trade_date date,
    ADD COLUMN IF NOT EXISTS entry_time timestamp,
    ADD COLUMN IF NOT EXISTS exit_time timestamp,
    ADD COLUMN IF NOT EXISTS side varchar(20),
    ADD COLUMN IF NOT EXISTS setup varchar(50),
    ADD COLUMN IF NOT EXISTS session varchar(20),
    ADD COLUMN IF NOT EXISTS entry_price numeric(18, 6),
    ADD COLUMN IF NOT EXISTS stop_price numeric(18, 6),
    ADD COLUMN IF NOT EXISTS target_price numeric(18, 6),
    ADD COLUMN IF NOT EXISTS exit_price numeric(18, 6),
    ADD COLUMN IF NOT EXISTS quantity integer,
    ADD COLUMN IF NOT EXISTS risk_amount numeric(18, 2),
    ADD COLUMN IF NOT EXISTS pnl_amount numeric(18, 2),
    ADD COLUMN IF NOT EXISTS pnl_r numeric(10, 2),
    ADD COLUMN IF NOT EXISTS outcome varchar(20),
    ADD COLUMN IF NOT EXISTS confidence integer,
    ADD COLUMN IF NOT EXISTS confidence_rating integer,
    ADD COLUMN IF NOT EXISTS emotion varchar(50),
    ADD COLUMN IF NOT EXISTS execution_rating integer,
    ADD COLUMN IF NOT EXISTS followed_plan boolean,
    ADD COLUMN IF NOT EXISTS mistake_tag varchar(50),
    ADD COLUMN IF NOT EXISTS notes text,
    ADD COLUMN IF NOT EXISTS psychology_notes text,
    ADD COLUMN IF NOT EXISTS lesson_learned text,
    ADD COLUMN IF NOT EXISTS screenshot_url varchar(500),
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS account_id bigint REFERENCES trading_accounts(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS import_batch_id bigint REFERENCES import_batches(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS external_trade_key varchar(255);

ALTER TABLE trades
    ALTER COLUMN setup DROP NOT NULL;

CREATE TABLE IF NOT EXISTS journal_entries (
    id bigserial PRIMARY KEY,
    user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    account_id bigint REFERENCES trading_accounts(id) ON DELETE SET NULL,
    journal_date date NOT NULL,
    mood varchar(50),
    setup_quality varchar(100),
    mistakes varchar(1000),
    lesson_learned varchar(1000),
    notes varchar(2000),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

ALTER TABLE journal_entries
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS account_id bigint REFERENCES trading_accounts(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS journal_date date,
    ADD COLUMN IF NOT EXISTS mood varchar(50),
    ADD COLUMN IF NOT EXISTS setup_quality varchar(100),
    ADD COLUMN IF NOT EXISTS mistakes varchar(1000),
    ADD COLUMN IF NOT EXISTS lesson_learned varchar(1000),
    ADD COLUMN IF NOT EXISTS notes varchar(2000),
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS ai_reflections (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date date NOT NULL,
    end_date date NOT NULL,
    prompt_text text,
    response_text text NOT NULL,
    created_at timestamp NOT NULL DEFAULT now()
);

ALTER TABLE ai_reflections
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS start_date date,
    ADD COLUMN IF NOT EXISTS end_date date,
    ADD COLUMN IF NOT EXISTS prompt_text text,
    ADD COLUMN IF NOT EXISTS response_text text,
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS ai_trade_reviews (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trade_id bigint REFERENCES trades(id) ON DELETE CASCADE,
    account_id bigint REFERENCES trading_accounts(id) ON DELETE SET NULL,
    review_date varchar(255),
    symbol varchar(255),
    pnl double precision,
    grade varchar(255),
    review_version varchar(255),
    review_hash varchar(255),
    summary text,
    mistakes_json text,
    strengths_json text,
    improvement_plan_json text,
    created_at timestamptz,
    status varchar(255),
    error_message text,
    tags_json text
);

ALTER TABLE ai_trade_reviews
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS trade_id bigint REFERENCES trades(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS account_id bigint REFERENCES trading_accounts(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS review_date varchar(255),
    ADD COLUMN IF NOT EXISTS symbol varchar(255),
    ADD COLUMN IF NOT EXISTS pnl double precision,
    ADD COLUMN IF NOT EXISTS grade varchar(255),
    ADD COLUMN IF NOT EXISTS review_version varchar(255),
    ADD COLUMN IF NOT EXISTS review_hash varchar(255),
    ADD COLUMN IF NOT EXISTS summary text,
    ADD COLUMN IF NOT EXISTS mistakes_json text,
    ADD COLUMN IF NOT EXISTS strengths_json text,
    ADD COLUMN IF NOT EXISTS improvement_plan_json text,
    ADD COLUMN IF NOT EXISTS created_at timestamptz,
    ADD COLUMN IF NOT EXISTS status varchar(255),
    ADD COLUMN IF NOT EXISTS error_message text,
    ADD COLUMN IF NOT EXISTS tags_json text;

CREATE TABLE IF NOT EXISTS ai_trade_analyses (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trade_id bigint NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
    review_version varchar(32) NOT NULL,
    review_hash varchar(128) NOT NULL,
    status varchar(32) NOT NULL,
    symbol varchar(32),
    pnl numeric(14, 2),
    grade varchar(8),
    execution_score integer,
    discipline_score integer,
    risk_score integer,
    summary text,
    market_context text,
    entry_analysis text,
    exit_analysis text,
    risk_analysis text,
    psychology_analysis text,
    chart_analysis text,
    mistakes_json text,
    strengths_json text,
    improvement_plan_json text,
    tags_json text,
    has_chart_image boolean NOT NULL DEFAULT false,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE ai_trade_analyses
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS trade_id bigint REFERENCES trades(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS review_version varchar(32),
    ADD COLUMN IF NOT EXISTS review_hash varchar(128),
    ADD COLUMN IF NOT EXISTS status varchar(32),
    ADD COLUMN IF NOT EXISTS symbol varchar(32),
    ADD COLUMN IF NOT EXISTS pnl numeric(14, 2),
    ADD COLUMN IF NOT EXISTS grade varchar(8),
    ADD COLUMN IF NOT EXISTS execution_score integer,
    ADD COLUMN IF NOT EXISTS discipline_score integer,
    ADD COLUMN IF NOT EXISTS risk_score integer,
    ADD COLUMN IF NOT EXISTS summary text,
    ADD COLUMN IF NOT EXISTS market_context text,
    ADD COLUMN IF NOT EXISTS entry_analysis text,
    ADD COLUMN IF NOT EXISTS exit_analysis text,
    ADD COLUMN IF NOT EXISTS risk_analysis text,
    ADD COLUMN IF NOT EXISTS psychology_analysis text,
    ADD COLUMN IF NOT EXISTS chart_analysis text,
    ADD COLUMN IF NOT EXISTS mistakes_json text,
    ADD COLUMN IF NOT EXISTS strengths_json text,
    ADD COLUMN IF NOT EXISTS improvement_plan_json text,
    ADD COLUMN IF NOT EXISTS tags_json text,
    ADD COLUMN IF NOT EXISTS has_chart_image boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS error_message text,
    ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS ai_coach_snapshot (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id bigint NOT NULL REFERENCES trading_accounts(id) ON DELETE CASCADE,
    snapshot_type varchar(20) NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    status varchar(20) NOT NULL,
    total_trades integer,
    net_pnl numeric(12, 2),
    win_rate numeric(6, 2),
    profit_factor numeric(8, 2),
    best_setup varchar(100),
    worst_setup varchar(100),
    best_session varchar(50),
    worst_session varchar(50),
    biggest_mistake varchar(255),
    deterministic_summary_json text,
    ai_response_json text,
    review_hash varchar(128),
    error_message varchar(1000),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

ALTER TABLE ai_coach_snapshot
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS account_id bigint REFERENCES trading_accounts(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS snapshot_type varchar(20),
    ADD COLUMN IF NOT EXISTS period_start date,
    ADD COLUMN IF NOT EXISTS period_end date,
    ADD COLUMN IF NOT EXISTS status varchar(20),
    ADD COLUMN IF NOT EXISTS total_trades integer,
    ADD COLUMN IF NOT EXISTS net_pnl numeric(12, 2),
    ADD COLUMN IF NOT EXISTS win_rate numeric(6, 2),
    ADD COLUMN IF NOT EXISTS profit_factor numeric(8, 2),
    ADD COLUMN IF NOT EXISTS best_setup varchar(100),
    ADD COLUMN IF NOT EXISTS worst_setup varchar(100),
    ADD COLUMN IF NOT EXISTS best_session varchar(50),
    ADD COLUMN IF NOT EXISTS worst_session varchar(50),
    ADD COLUMN IF NOT EXISTS biggest_mistake varchar(255),
    ADD COLUMN IF NOT EXISTS deterministic_summary_json text,
    ADD COLUMN IF NOT EXISTS ai_response_json text,
    ADD COLUMN IF NOT EXISTS review_hash varchar(128),
    ADD COLUMN IF NOT EXISTS error_message varchar(1000),
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS daily_discipline_scores (
    id bigserial PRIMARY KEY,
    user_id bigint,
    trade_date date,
    account_id bigint,
    score integer,
    total_trades integer,
    violation_count integer,
    major_violation_count integer,
    critical_violation_count integer,
    current_clean_day_streak integer,
    clean_day boolean,
    created_at timestamp,
    updated_at timestamp
);

ALTER TABLE daily_discipline_scores
    ADD COLUMN IF NOT EXISTS user_id bigint,
    ADD COLUMN IF NOT EXISTS trade_date date,
    ADD COLUMN IF NOT EXISTS account_id bigint,
    ADD COLUMN IF NOT EXISTS score integer,
    ADD COLUMN IF NOT EXISTS total_trades integer,
    ADD COLUMN IF NOT EXISTS violation_count integer,
    ADD COLUMN IF NOT EXISTS major_violation_count integer,
    ADD COLUMN IF NOT EXISTS critical_violation_count integer,
    ADD COLUMN IF NOT EXISTS current_clean_day_streak integer,
    ADD COLUMN IF NOT EXISTS clean_day boolean,
    ADD COLUMN IF NOT EXISTS created_at timestamp,
    ADD COLUMN IF NOT EXISTS updated_at timestamp;

CREATE TABLE IF NOT EXISTS trade_rule_violations (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trade_id bigint NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
    rule_code varchar(80) NOT NULL,
    rule_title varchar(255) NOT NULL,
    category varchar(50) NOT NULL,
    severity varchar(20) NOT NULL,
    reason text,
    source varchar(30) NOT NULL,
    trade_date date,
    created_at timestamp
);

ALTER TABLE trade_rule_violations
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS trade_id bigint REFERENCES trades(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS rule_code varchar(80),
    ADD COLUMN IF NOT EXISTS rule_title varchar(255),
    ADD COLUMN IF NOT EXISTS category varchar(50),
    ADD COLUMN IF NOT EXISTS severity varchar(20),
    ADD COLUMN IF NOT EXISTS reason text,
    ADD COLUMN IF NOT EXISTS source varchar(30),
    ADD COLUMN IF NOT EXISTS trade_date date,
    ADD COLUMN IF NOT EXISTS created_at timestamp;

CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_plan ON users(plan);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_lower_email ON users(lower(email));
CREATE INDEX IF NOT EXISTS idx_users_lower_name ON users(lower(name));
CREATE INDEX IF NOT EXISTS idx_users_stripe_customer_id ON users(stripe_customer_id);
CREATE INDEX IF NOT EXISTS idx_users_stripe_subscription_id ON users(stripe_subscription_id);

CREATE INDEX IF NOT EXISTS idx_trading_accounts_user_id ON trading_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_import_batches_user_id ON import_batches(user_id);
CREATE INDEX IF NOT EXISTS idx_import_batches_account_id ON import_batches(account_id);
CREATE INDEX IF NOT EXISTS idx_trades_user_id ON trades(user_id);
CREATE INDEX IF NOT EXISTS idx_trades_account_id ON trades(account_id);
CREATE INDEX IF NOT EXISTS idx_trades_user_account_date ON trades(user_id, account_id, trade_date);
CREATE INDEX IF NOT EXISTS idx_trades_user_entry_time ON trades(user_id, entry_time);
CREATE INDEX IF NOT EXISTS idx_trades_account_entry_time ON trades(account_id, entry_time);
CREATE INDEX IF NOT EXISTS idx_trades_user_account_entry_time ON trades(user_id, account_id, entry_time);
CREATE UNIQUE INDEX IF NOT EXISTS uk_trades_user_account_external_key
    ON trades(user_id, account_id, external_trade_key)
    WHERE external_trade_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_journal_entries_user_id ON journal_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_journal_entries_user_account_date ON journal_entries(user_id, account_id, journal_date);
CREATE INDEX IF NOT EXISTS idx_ai_reflections_user_id ON ai_reflections(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_reflections_user_period ON ai_reflections(user_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_ai_trade_reviews_user_id ON ai_trade_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_trade_reviews_trade_id ON ai_trade_reviews(trade_id);
CREATE INDEX IF NOT EXISTS idx_ai_trade_reviews_account_id ON ai_trade_reviews(account_id);
CREATE INDEX IF NOT EXISTS idx_ai_trade_reviews_cache ON ai_trade_reviews(user_id, trade_id, review_version, review_hash);
DELETE FROM ai_trade_reviews duplicate
USING ai_trade_reviews keeper
WHERE duplicate.id > keeper.id
  AND duplicate.user_id = keeper.user_id
  AND duplicate.trade_id = keeper.trade_id
  AND duplicate.review_version = keeper.review_version
  AND duplicate.review_hash = keeper.review_hash
  AND duplicate.trade_id IS NOT NULL
  AND duplicate.review_version IS NOT NULL
  AND duplicate.review_hash IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_trade_reviews_cache
    ON ai_trade_reviews(user_id, trade_id, review_version, review_hash);
CREATE INDEX IF NOT EXISTS idx_ai_trade_analysis_user_id ON ai_trade_analyses(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_trade_analysis_trade_id ON ai_trade_analyses(trade_id);
CREATE INDEX IF NOT EXISTS idx_ai_trade_analysis_hash ON ai_trade_analyses(review_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_trade_analysis_cache
    ON ai_trade_analyses(user_id, trade_id, review_version, review_hash);
CREATE INDEX IF NOT EXISTS idx_ai_coach_snapshot_user_id ON ai_coach_snapshot(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_coach_snapshot_account_id ON ai_coach_snapshot(account_id);
CREATE INDEX IF NOT EXISTS idx_ai_coach_snapshot_user_account_type_period
    ON ai_coach_snapshot(user_id, account_id, snapshot_type, period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_daily_discipline_scores_user_id ON daily_discipline_scores(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_discipline_scores_user_account_date
    ON daily_discipline_scores(user_id, account_id, trade_date);
CREATE INDEX IF NOT EXISTS idx_trade_rule_violations_user_id ON trade_rule_violations(user_id);
CREATE INDEX IF NOT EXISTS idx_trade_rule_violations_trade_id ON trade_rule_violations(trade_id);
CREATE INDEX IF NOT EXISTS idx_trade_rule_violations_user_trade ON trade_rule_violations(user_id, trade_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_journal_user_account_date'
    ) THEN
        ALTER TABLE journal_entries
            ADD CONSTRAINT uk_journal_user_account_date UNIQUE (user_id, account_id, journal_date);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_ai_coach_snapshot'
    ) THEN
        ALTER TABLE ai_coach_snapshot
            ADD CONSTRAINT uk_ai_coach_snapshot
            UNIQUE (user_id, account_id, snapshot_type, period_start, period_end, review_hash);
    END IF;
END $$;

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_plan;
ALTER TABLE users
    ADD CONSTRAINT chk_users_plan CHECK (plan IN ('FREE', 'PRO'));

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_subscription_status;
ALTER TABLE users
    ADD CONSTRAINT chk_users_subscription_status
    CHECK (subscription_status IN ('ACTIVE', 'TRIALING', 'PAST_DUE', 'CANCELED', 'NONE'));

ALTER TABLE trading_accounts DROP CONSTRAINT IF EXISTS chk_trading_accounts_prop_firm;
ALTER TABLE trading_accounts
    ADD CONSTRAINT chk_trading_accounts_prop_firm
    CHECK (prop_firm IN ('TOPSTEP', 'APEX', 'TAKE_PROFIT_TRADER', 'TRADEIFY', 'OTHER'));

ALTER TABLE trading_accounts DROP CONSTRAINT IF EXISTS chk_trading_accounts_account_type;
ALTER TABLE trading_accounts
    ADD CONSTRAINT chk_trading_accounts_account_type
    CHECK (account_type IS NULL OR account_type IN ('FUNDED', 'EVALUATION', 'PERSONAL'));

ALTER TABLE trading_accounts DROP CONSTRAINT IF EXISTS chk_trading_accounts_status;
ALTER TABLE trading_accounts
    ADD CONSTRAINT chk_trading_accounts_status
    CHECK (status IN ('ACTIVE', 'PASSED', 'BLOWN', 'FUNDED', 'ARCHIVED'));

ALTER TABLE trades DROP CONSTRAINT IF EXISTS chk_trades_setup;
ALTER TABLE trades
    ADD CONSTRAINT chk_trades_setup
    CHECK (setup IS NULL OR setup IN ('FVG', 'IFVG', 'ORB', 'LIQUIDITY_SWEEP', 'BREAKER', 'SMT', 'RECLAIM', 'TREND_CONTINUATION', 'RANGE_REVERSION', 'COUNTER_TREND', 'OTHER'));

ALTER TABLE trades DROP CONSTRAINT IF EXISTS chk_trades_confidence;
ALTER TABLE trades
    ADD CONSTRAINT chk_trades_confidence
    CHECK (confidence IS NULL OR confidence BETWEEN 1 AND 5);

ALTER TABLE trades DROP CONSTRAINT IF EXISTS chk_trades_session;
ALTER TABLE trades
    ADD CONSTRAINT chk_trades_session
    CHECK (session IN ('ASIA', 'LONDON', 'NY_AM', 'NY_PM'));

ALTER TABLE trades DROP CONSTRAINT IF EXISTS chk_trades_outcome;
ALTER TABLE trades
    ADD CONSTRAINT chk_trades_outcome
    CHECK (outcome IN ('WIN', 'LOSS', 'BE'));

ALTER TABLE trades DROP CONSTRAINT IF EXISTS chk_trades_mistake_tag;
ALTER TABLE trades
    ADD CONSTRAINT chk_trades_mistake_tag
    CHECK (mistake_tag IS NULL OR mistake_tag IN (
        'NONE',
        'EARLY_ENTRY',
        'LATE_ENTRY',
        'EARLY_EXIT',
        'MOVED_TO_BE_TOO_EARLY',
        'OVERTRADING',
        'COUNTER_BIAS',
        'FOMO',
        'REVENGE_TRADE',
        'NO_CONFIRMATION',
        'OTHER'
    ));

ALTER TABLE ai_coach_snapshot DROP CONSTRAINT IF EXISTS chk_ai_coach_snapshot_type;
ALTER TABLE ai_coach_snapshot
    ADD CONSTRAINT chk_ai_coach_snapshot_type CHECK (snapshot_type IN ('DAILY', 'WEEKLY'));

ALTER TABLE ai_coach_snapshot DROP CONSTRAINT IF EXISTS chk_ai_coach_snapshot_status;
ALTER TABLE ai_coach_snapshot
    ADD CONSTRAINT chk_ai_coach_snapshot_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));
