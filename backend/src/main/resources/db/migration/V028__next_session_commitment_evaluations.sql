create table next_session_protection_evaluations (
 id bigserial primary key, user_id bigint not null references users(id) on delete cascade,
 account_id bigint not null references trading_accounts(id) on delete cascade,
 commitment_id bigint not null references next_session_protection_commitments(id) on delete cascade,
 evaluated_session_date date not null, overall_status varchar(32) not null,
 source_fingerprint varchar(128) not null, evaluation_version varchar(40) not null,
 evidence_coverage integer not null, is_current boolean not null default true,
 created_at timestamp not null default current_timestamp, updated_at timestamp not null default current_timestamp,
 constraint uk_protection_evaluation_fingerprint unique(commitment_id, source_fingerprint)
);
create index idx_protection_evaluations_user_account_date on next_session_protection_evaluations(user_id, account_id, evaluated_session_date);
create index idx_protection_evaluations_commitment_current on next_session_protection_evaluations(commitment_id, is_current);
create table next_session_protection_rule_results (
 id bigserial primary key, evaluation_id bigint not null references next_session_protection_evaluations(id) on delete cascade,
 rule_type varchar(48) not null, result varchar(32) not null, configured_value varchar(120), observed_value varchar(120), evidence text not null, evidence_count integer not null default 0, calculation_version varchar(40) not null,
 constraint uk_protection_rule_result unique(evaluation_id, rule_type)
);
create index idx_protection_rule_results_evaluation on next_session_protection_rule_results(evaluation_id);
