-- TradePilot observability tables

create table if not exists api_request_metrics (
  id bigserial primary key,
  endpoint varchar(220) not null,
  method varchar(16) not null,
  status integer not null,
  duration_ms bigint not null,
  user_id bigint null references users(id) on delete set null,
  created_at timestamp not null default now()
);

create index if not exists idx_api_request_metrics_created_at on api_request_metrics(created_at);
create index if not exists idx_api_request_metrics_endpoint_created_at on api_request_metrics(endpoint, created_at);
create index if not exists idx_api_request_metrics_status_created_at on api_request_metrics(status, created_at);
create index if not exists idx_api_request_metrics_user_id_created_at on api_request_metrics(user_id, created_at);

create table if not exists slow_request_events (
  id bigserial primary key,
  endpoint varchar(220) not null,
  duration_ms bigint not null,
  status integer not null,
  user_id bigint null references users(id) on delete set null,
  severity varchar(16) not null,
  created_at timestamp not null default now()
);

create index if not exists idx_slow_request_events_created_at on slow_request_events(created_at);
create index if not exists idx_slow_request_events_endpoint_created_at on slow_request_events(endpoint, created_at);

create table if not exists ai_request_metrics (
  id bigserial primary key,
  user_id bigint null references users(id) on delete set null,
  feature varchar(64) not null,
  model varchar(80) not null,
  prompt_tokens integer null,
  completion_tokens integer null,
  total_tokens integer null,
  estimated_cost numeric(12, 6) null,
  duration_ms bigint not null,
  success boolean not null,
  created_at timestamp not null default now()
);

create index if not exists idx_ai_request_metrics_created_at on ai_request_metrics(created_at);
create index if not exists idx_ai_request_metrics_user_id_created_at on ai_request_metrics(user_id, created_at);
create index if not exists idx_ai_request_metrics_feature_created_at on ai_request_metrics(feature, created_at);

create table if not exists cache_metrics (
  id bigserial primary key,
  cache_name varchar(120) not null,
  hit boolean not null,
  created_at timestamp not null default now()
);

create index if not exists idx_cache_metrics_cache_name_created_at on cache_metrics(cache_name, created_at);
create index if not exists idx_cache_metrics_created_at on cache_metrics(created_at);

create table if not exists email_delivery_events (
  id bigserial primary key,
  recipient varchar(240) not null,
  template varchar(120) not null,
  success boolean not null,
  duration_ms bigint not null,
  error_message varchar(700) null,
  created_at timestamp not null default now()
);

create index if not exists idx_email_delivery_events_created_at on email_delivery_events(created_at);
create index if not exists idx_email_delivery_events_success_created_at on email_delivery_events(success, created_at);

create table if not exists stripe_events (
  id bigserial primary key,
  event_type varchar(120) not null,
  success boolean not null,
  duration_ms bigint not null,
  error_message varchar(700) null,
  created_at timestamp not null default now()
);

create index if not exists idx_stripe_events_created_at on stripe_events(created_at);
create index if not exists idx_stripe_events_event_type_created_at on stripe_events(event_type, created_at);
create index if not exists idx_stripe_events_success_created_at on stripe_events(success, created_at);

create table if not exists processed_stripe_events (
  id bigserial primary key,
  event_id varchar(120) not null unique,
  event_type varchar(120) not null,
  processed_at timestamp not null default now()
);

create index if not exists idx_processed_stripe_events_processed_at on processed_stripe_events(processed_at);
create index if not exists idx_processed_stripe_events_event_type_processed_at on processed_stripe_events(event_type, processed_at);

create table if not exists query_performance_events (
  id bigserial primary key,
  query_name varchar(140) not null,
  duration_ms bigint not null,
  created_at timestamp not null default now()
);

create index if not exists idx_query_performance_events_created_at on query_performance_events(created_at);
create index if not exists idx_query_performance_events_query_name_created_at on query_performance_events(query_name, created_at);

create table if not exists system_alerts (
  id bigserial primary key,
  type varchar(80) not null,
  severity varchar(24) not null,
  message varchar(700) not null,
  status varchar(24) not null default 'OPEN',
  resolved boolean not null default false,
  created_at timestamp not null default now(),
  resolved_at timestamp null,
  resolved_by bigint null references users(id) on delete set null
);

alter table system_alerts add column if not exists status varchar(24) not null default 'OPEN';
alter table system_alerts add column if not exists resolved_at timestamp null;
alter table system_alerts add column if not exists resolved_by bigint null references users(id) on delete set null;
update system_alerts set status = case when resolved = true then 'RESOLVED' else 'OPEN' end where status is null;

create index if not exists idx_system_alerts_resolved_created_at on system_alerts(resolved, created_at);
create index if not exists idx_system_alerts_type_created_at on system_alerts(type, created_at);
create index if not exists idx_system_alerts_severity_created_at on system_alerts(severity, created_at);
create index if not exists idx_system_alerts_status_created_at on system_alerts(status, created_at);
create index if not exists idx_system_alerts_resolved_at on system_alerts(resolved_at);
create index if not exists idx_system_alerts_resolved_by_created_at on system_alerts(resolved_by, created_at);

-- Supporting indexes for slow admin observability endpoints.
create index if not exists idx_api_request_metrics_created_duration on api_request_metrics(created_at, duration_ms);
create index if not exists idx_api_request_metrics_endpoint_created_at on api_request_metrics(endpoint, created_at);
create index if not exists idx_api_request_metrics_status_created_at on api_request_metrics(status, created_at);
create index if not exists idx_slow_request_events_created_at_desc on slow_request_events(created_at desc);
create index if not exists idx_feature_usage_events_created_user on feature_usage_events(created_at, user_id);
create index if not exists idx_feature_usage_events_user_created on feature_usage_events(user_id, created_at);
create index if not exists idx_ai_usage_events_user_type_created on ai_usage_events(user_id, usage_type, created_at);
create index if not exists idx_trading_accounts_user_id on trading_accounts(user_id);
create index if not exists idx_trades_user_id on trades(user_id);
create index if not exists idx_ai_trade_reviews_user_id on ai_trade_reviews(user_id);
create index if not exists idx_users_plan on users(plan);
create index if not exists idx_users_role on users(role);
create index if not exists idx_users_created_at on users(created_at);
create index if not exists idx_users_lower_email on users(lower(email));
create index if not exists idx_users_lower_name on users(lower(name));
