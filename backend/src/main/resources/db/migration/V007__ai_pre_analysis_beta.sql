create table if not exists user_feature_entitlements (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    entitlement_key varchar(80) not null,
    enabled boolean not null default true,
    reason varchar(280),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_user_feature_entitlements_user_key unique (user_id, entitlement_key)
);

create index if not exists idx_user_feature_entitlements_user on user_feature_entitlements(user_id);
create index if not exists idx_user_feature_entitlements_key on user_feature_entitlements(entitlement_key);

create table if not exists trading_playbooks (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    name varchar(160) not null,
    description text,
    markets jsonb not null default '[]'::jsonb,
    context_timeframes jsonb not null default '[]'::jsonb,
    setup_timeframes jsonb not null default '[]'::jsonb,
    execution_timeframes jsonb not null default '[]'::jsonb,
    setup_sequence jsonb not null default '[]'::jsonb,
    entry_rules jsonb not null default '[]'::jsonb,
    invalidation_rules jsonb not null default '[]'::jsonb,
    target_rules jsonb not null default '[]'::jsonb,
    avoid_rules jsonb not null default '[]'::jsonb,
    custom_notes text,
    is_active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index if not exists idx_trading_playbooks_user_active on trading_playbooks(user_id, is_active);
create index if not exists idx_trading_playbooks_user_created on trading_playbooks(user_id, created_at desc);

create table if not exists ai_pre_analyses (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    playbook_id bigint not null references trading_playbooks(id) on delete restrict,
    market varchar(12) not null,
    timeframe varchar(12) not null,
    image_reference varchar(120),
    image_content_type varchar(80),
    image_bytes bytea,
    trader_note text,
    setup_status varchar(40) not null,
    bias varchar(20),
    setup_alignment_score integer,
    structure_quality_score numeric(5,2),
    analysis_json jsonb not null,
    model_name varchar(120),
    prompt_version varchar(60) not null,
    created_at timestamp not null default now()
);

create index if not exists idx_ai_pre_analyses_user_created on ai_pre_analyses(user_id, created_at desc);
create index if not exists idx_ai_pre_analyses_playbook on ai_pre_analyses(playbook_id);

create table if not exists ai_pre_analysis_feedback (
    id bigserial primary key,
    analysis_id bigint not null references ai_pre_analyses(id) on delete cascade,
    user_id bigint not null references users(id) on delete cascade,
    rating integer not null,
    reason_code varchar(60),
    comment text,
    created_at timestamp not null default now(),
    constraint uk_ai_pre_analysis_feedback_analysis_user unique (analysis_id, user_id),
    constraint chk_ai_pre_analysis_feedback_rating check (rating in (-1, 1))
);

create index if not exists idx_ai_pre_analysis_feedback_user on ai_pre_analysis_feedback(user_id);
