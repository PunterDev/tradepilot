-- Shared PostgreSQL-backed rate limit windows for production multi-instance deployments.
-- The application updates rows atomically with insert ... on conflict so the database,
-- not JVM-local memory, is the authoritative limiter state.

create table if not exists rate_limit_windows (
    rate_key varchar(512) primary key,
    window_start timestamp not null,
    request_count integer not null default 0,
    expires_at timestamp not null,
    updated_at timestamp not null default now(),
    constraint chk_rate_limit_windows_request_count_nonnegative check (request_count >= 0)
);

create index if not exists idx_rate_limit_windows_expires_at
    on rate_limit_windows(expires_at);
