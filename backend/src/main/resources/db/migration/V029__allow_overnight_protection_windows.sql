-- Account Survival commitments deliberately support windows such as 18:00-03:00.
alter table next_session_protection_commitments drop constraint if exists ck_protection_commitment_window;
