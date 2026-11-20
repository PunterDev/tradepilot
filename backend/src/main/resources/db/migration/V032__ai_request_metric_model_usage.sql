alter table ai_request_metrics add column if not exists input_tokens integer;
alter table ai_request_metrics add column if not exists cached_input_tokens integer;
alter table ai_request_metrics add column if not exists output_tokens integer;
alter table ai_request_metrics add column if not exists reasoning_tokens integer;
alter table ai_request_metrics add column if not exists retry_attempt integer;
alter table ai_request_metrics add column if not exists error_category varchar(40);
