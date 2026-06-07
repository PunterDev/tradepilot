create table if not exists feature_usage_events (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    feature varchar(50) not null,
    created_at timestamp not null default now()
);

create index if not exists idx_feature_usage_user_id
    on feature_usage_events(user_id);

create index if not exists idx_feature_usage_created_at
    on feature_usage_events(created_at);

create index if not exists idx_feature_usage_feature_created
    on feature_usage_events(feature, created_at);

create index if not exists idx_feature_usage_user_feature_created
    on feature_usage_events(user_id, feature, created_at);

DO $$
DECLARE
    constraint_record record;
BEGIN
    FOR constraint_record IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'feature_usage_events'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%feature%'
    LOOP
        EXECUTE format('ALTER TABLE feature_usage_events DROP CONSTRAINT IF EXISTS %I', constraint_record.conname);
    END LOOP;
END $$;

ALTER TABLE feature_usage_events
    ADD CONSTRAINT chk_feature_usage_events_feature
    CHECK (feature IN (
        'AI_REVIEW',
        'DAILY_COACH',
        'WEEKLY_COACH',
        'TRADING_DNA',
        'SESSION_REVIEW',
        'TRADE_DETAIL',
        'CALENDAR_VIEW',
        'ANALYTICS_VIEW'
    ));
