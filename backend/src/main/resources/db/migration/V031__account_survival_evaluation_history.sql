-- Fingerprints identify canonical evidence, not globally unique events. The same evidence may
-- legitimately recur after an intervening evaluation and must remain append-only history.
alter table next_session_protection_evaluations
    drop constraint if exists uk_protection_evaluation_fingerprint;

-- Preserve one current evaluation per commitment/session while retaining every prior evidence version.
create unique index if not exists uk_protection_evaluation_current_session
    on next_session_protection_evaluations(commitment_id, evaluated_session_date)
    where is_current;

create index if not exists idx_protection_evaluations_commitment_history
    on next_session_protection_evaluations(commitment_id, id desc);

create index if not exists idx_protection_evaluations_account_history
    on next_session_protection_evaluations(user_id, account_id, id desc);
