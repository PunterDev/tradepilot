create table if not exists account_risk_profiles (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint not null references trading_accounts(id) on delete cascade,
    current_balance numeric(18,2),
    failure_threshold numeric(18,2),
    daily_loss_limit numeric(18,2),
    planned_risk_per_trade numeric(18,2),
    personal_max_contracts integer,
    firm_max_contracts integer,
    primary_window_start time,
    primary_window_end time,
    renewal_date date,
    payout_target numeric(18,2),
    payout_date date,
    timezone varchar(64),
    account_stage varchar(24),
    effective_date date,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_account_risk_profiles_account unique (account_id),
    constraint ck_account_risk_profiles_personal_size check (personal_max_contracts is null or personal_max_contracts > 0),
    constraint ck_account_risk_profiles_firm_size check (firm_max_contracts is null or firm_max_contracts > 0),
    constraint ck_account_risk_profiles_window check (primary_window_start is null or primary_window_end is null or primary_window_start < primary_window_end)
);

create index if not exists idx_account_risk_profiles_user_account on account_risk_profiles(user_id, account_id);

create table if not exists next_session_protection_commitments (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint not null references trading_accounts(id) on delete cascade,
    effective_session_date date not null,
    max_normalized_exposure numeric(10,2),
    max_attempts integer,
    max_session_loss numeric(18,2),
    post_loss_pause_minutes integer,
    allowed_window_start time,
    allowed_window_end time,
    require_fresh_setup_confirmation boolean,
    source varchar(32) not null,
    status varchar(32) not null,
    accepted_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_next_session_protection_account_date unique (account_id, effective_session_date),
    constraint ck_protection_commitment_attempts check (max_attempts is null or max_attempts > 0),
    constraint ck_protection_commitment_pause check (post_loss_pause_minutes is null or post_loss_pause_minutes >= 0),
    constraint ck_protection_commitment_window check (allowed_window_start is null or allowed_window_end is null or allowed_window_start < allowed_window_end)
);

create index if not exists idx_next_session_protection_user_account_date on next_session_protection_commitments(user_id, account_id, effective_session_date);
