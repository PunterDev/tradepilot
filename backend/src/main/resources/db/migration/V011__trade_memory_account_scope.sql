alter table trade_memories
    add column if not exists account_id bigint;

update trade_memories tm
set account_id = t.account_id
from trades t
where tm.trade_id = t.id
  and tm.user_id = t.user_id
  and tm.account_id is distinct from t.account_id;

create index if not exists idx_trade_memories_user_account_status
    on trade_memories(user_id, account_id, embedding_status);
