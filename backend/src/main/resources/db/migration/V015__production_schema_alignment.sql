-- Align the reconstructed V001 trade_memories baseline with the current JPA
-- contract. Invalid legacy UUID values deliberately abort this migration so
-- operators can repair the affected records without losing data.
do $$
declare
    invalid_count bigint;
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'trade_memories'
          and column_name = 'id'
          and data_type <> 'uuid'
    ) then
        select count(*)
        into invalid_count
        from trade_memories
        where id is not null
          and id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$';

        if invalid_count > 0 then
            raise exception 'Cannot convert trade_memories.id to UUID: % invalid value(s) require manual repair', invalid_count;
        end if;

        alter table trade_memories
            alter column id type uuid using id::uuid;
    end if;

    if exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'trade_memories'
          and column_name = 'vector_point_id'
          and data_type <> 'uuid'
    ) then
        select count(*)
        into invalid_count
        from trade_memories
        where vector_point_id is not null
          and vector_point_id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$';

        if invalid_count > 0 then
            raise exception 'Cannot convert trade_memories.vector_point_id to UUID: % invalid value(s) require manual repair', invalid_count;
        end if;

        alter table trade_memories
            alter column vector_point_id type uuid using vector_point_id::uuid;
    end if;

    if exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'trade_memories'
          and column_name = 'content'
    ) and not exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'trade_memories'
          and column_name = 'summary'
    ) then
        alter table trade_memories rename column content to summary;
    elsif exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'trade_memories'
          and column_name = 'content'
    ) and exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'trade_memories'
          and column_name = 'summary'
    ) then
        update trade_memories
        set summary = content
        where summary is null;
    end if;

    if not exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'trade_memories'
          and column_name = 'summary'
    ) then
        raise exception 'trade_memories.summary is required by the current JPA mapping';
    end if;

    if exists (select 1 from trade_memories where summary is null) then
        raise exception 'Cannot set trade_memories.summary NOT NULL while null values remain';
    end if;

    alter table trade_memories alter column summary set not null;
end $$;

do $$
declare
    timestamp_column text;
begin
    foreach timestamp_column in array array[
        'last_attempted_at',
        'next_retry_at',
        'created_at',
        'updated_at',
        'embedded_at'
    ] loop
        if exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'trade_memories'
              and column_name = timestamp_column
              and data_type = 'timestamp without time zone'
        ) then
            execute format(
                'alter table trade_memories alter column %1$I type timestamp with time zone using %1$I at time zone ''UTC''',
                timestamp_column
            );
        end if;
    end loop;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conrelid = 'trade_memories'::regclass
          and conname = 'uk_trade_memories_trade_id'
    ) then
        alter table trade_memories
            add constraint uk_trade_memories_trade_id unique (trade_id);
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conrelid = 'trade_memories'::regclass
          and conname = 'uk_trade_memories_vector_point_id'
    ) then
        alter table trade_memories
            add constraint uk_trade_memories_vector_point_id unique (vector_point_id);
    end if;
end $$;

-- Hibernate's historical update mode created token_hash as VARCHAR(64), while
-- the reconstructed baseline used VARCHAR(255). Reject oversized legacy data
-- instead of allowing a narrowing conversion to discard characters.
do $$
declare
    oversized_count bigint;
begin
    select count(*)
    into oversized_count
    from auth_tokens
    where length(token_hash) > 64;

    if oversized_count > 0 then
        raise exception 'Cannot narrow auth_tokens.token_hash to VARCHAR(64): % oversized value(s) require manual repair', oversized_count;
    end if;

    alter table auth_tokens
        alter column token_hash type varchar(64) using token_hash::varchar(64);
end $$;

-- V001 retained the pre-JPA-default trade-rule schema. Align its explicit
-- lengths, required columns, and nullability without silently truncating or
-- manufacturing historical values.
alter table trade_rule_violations
    add column if not exists trade_date date;

do $$
declare
    column_name text;
    maximum_length integer;
    oversized_count bigint;
begin
    for column_name, maximum_length in
        select * from (values
            ('rule_code', 80),
            ('category', 50),
            ('severity', 20),
            ('source', 30)
        ) as length_limits(column_name, maximum_length)
    loop
        execute format(
            'select count(*) from trade_rule_violations where length(%1$I) > %2$s',
            column_name,
            maximum_length
        ) into oversized_count;

        if oversized_count > 0 then
            raise exception 'Cannot narrow trade_rule_violations.% to VARCHAR(%): % oversized value(s) require manual repair',
                column_name, maximum_length, oversized_count;
        end if;
    end loop;

    if exists (
        select 1
        from trade_rule_violations
        where user_id is null
           or trade_id is null
           or rule_code is null
           or rule_title is null
           or category is null
           or severity is null
           or source is null
    ) then
        raise exception 'Cannot apply required trade_rule_violations JPA contract while null required values remain';
    end if;
end $$;

alter table trade_rule_violations
    alter column rule_code type varchar(80) using rule_code::varchar(80),
    alter column category type varchar(50) using category::varchar(50),
    alter column severity type varchar(20) using severity::varchar(20),
    alter column source type varchar(30) using source::varchar(30),
    alter column user_id set not null,
    alter column trade_id set not null,
    alter column rule_code set not null,
    alter column rule_title set not null,
    alter column category set not null,
    alter column severity set not null,
    alter column source set not null;

do $$
begin
    if exists (
        select 1
        from performance_milestones
        where period_start is null
           or period_end is null
    ) then
        raise exception 'Cannot apply required performance_milestones JPA contract while null period values remain';
    end if;
end $$;

alter table performance_milestones
    alter column period_start set not null,
    alter column period_end set not null;

create index if not exists idx_performance_milestones_period
    on performance_milestones(account_id, period_start);

create index if not exists idx_performance_timeline_period
    on performance_timeline_snapshots(account_id, period_start);

do $$
declare
    oversized_count bigint;
begin
    select count(*)
    into oversized_count
    from trade_replay_insights
    where length(insight_type) > 50;

    if oversized_count > 0 then
        raise exception 'Cannot narrow trade_replay_insights.insight_type to VARCHAR(50): % oversized value(s) require manual repair', oversized_count;
    end if;
end $$;

alter table trade_replay_insights
    alter column insight_type type varchar(50) using insight_type::varchar(50);
