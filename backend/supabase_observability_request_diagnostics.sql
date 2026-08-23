-- Adds request-level support diagnostics for the production monitoring dashboard.
-- Safe to run repeatedly in Supabase.

alter table api_request_metrics
    add column if not exists request_id varchar(80);

alter table api_request_metrics
    add column if not exists error_code varchar(80);

create index if not exists idx_api_request_metrics_request_id
    on api_request_metrics(request_id);

create index if not exists idx_api_request_metrics_error_code_created_at
    on api_request_metrics(error_code, created_at);
