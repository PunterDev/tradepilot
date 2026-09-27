-- RequestMetric correlation and error fields predate the Flyway baseline.
-- Add them forward-only so fresh schemas match the current JPA mapping.
alter table api_request_metrics
    add column if not exists request_id varchar(80);

alter table api_request_metrics
    add column if not exists error_code varchar(80);

create index if not exists idx_api_request_metrics_request_id
    on api_request_metrics(request_id);
