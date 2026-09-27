-- AiUsageEvent has always supported an optional polymorphic resource reference.
-- Existing databases may already have this legacy Hibernate-created column; new
-- Flyway baselines need it before Hibernate validation starts.
alter table ai_usage_events
    add column if not exists related_resource_id bigint;

-- The JPA enum mapping is varchar(40). V001 used a wider legacy definition.
alter table ai_usage_events
    alter column usage_type type varchar(40);

-- V001 also included an unmapped required counter. Keep any historical data but
-- allow JPA inserts, which intentionally do not supply this legacy column.
do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'ai_usage_events'
          and column_name = 'credits_used'
    ) then
        alter table ai_usage_events
            alter column credits_used drop not null;
    end if;
end $$;
