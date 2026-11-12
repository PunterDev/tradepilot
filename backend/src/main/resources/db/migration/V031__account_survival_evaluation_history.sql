-- Keep one current evaluation per commitment/session while preserving every prior source version.
create unique index if not exists uk_protection_evaluation_current_session
    on next_session_protection_evaluations(commitment_id, evaluated_session_date)
    where is_current;

create index if not exists idx_protection_evaluations_commitment_session_current
    on next_session_protection_evaluations(commitment_id, evaluated_session_date desc, is_current);
