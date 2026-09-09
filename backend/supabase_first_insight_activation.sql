-- First Insight Activation rollout support for existing Supabase/PostgreSQL databases.
-- Safe to run after the existing feature usage funnel migrations.

create table if not exists user_feature_previews (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    feature_key varchar(80) not null,
    status varchar(20) not null default 'AVAILABLE',
    granted_at timestamp not null default now(),
    consumed_at timestamp,
    reserved_at timestamp,
    reserved_resource_id bigint,
    expires_at timestamp,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_user_feature_previews_user_feature unique (user_id, feature_key),
    constraint chk_user_feature_previews_status check (status in ('AVAILABLE', 'RESERVED', 'CONSUMED', 'EXPIRED'))
);

create index if not exists idx_user_feature_previews_user_id on user_feature_previews(user_id);
create index if not exists idx_user_feature_previews_status on user_feature_previews(status);
create index if not exists idx_user_feature_previews_user_status on user_feature_previews(user_id, status);

alter table feature_usage_events
    drop constraint if exists chk_feature_usage_events_feature;

alter table feature_usage_events
    add constraint chk_feature_usage_events_feature
    check (feature in (
        'ACTIVATION',
        'AI_REVIEW',
        'DAILY_COACH',
        'WEEKLY_COACH',
        'TRADING_DNA',
        'AI_LEAK_DETECTOR',
        'PERFORMANCE_TIMELINE',
        'PLAYBOOK',
        'TRADING_MEMORY',
        'SESSION_REVIEW',
        'TRADE_REPLAY',
        'TRADE_DETAIL',
        'CALENDAR_VIEW',
        'ANALYTICS_VIEW'
    ));

alter table feature_usage_events
    drop constraint if exists feature_usage_events_action_check;
alter table feature_usage_events
    drop constraint if exists chk_feature_usage_events_action;

alter table feature_usage_events
    add constraint feature_usage_events_action_check
    check (
        action is null
        or action in (
            'UNKNOWN_ACTION',
            'PAGE_VIEWED',
            'STARTED',
            'CREATED',
            'CONFIGURED',
            'LINKED',
            'REVIEWED',
            'GENERATED',
            'COMPLETED',
            'RETURNED',
            'UPGRADED',
            'RENEWED',
            'JOURNEY_VIEWED',
            'JOURNEY_CHAPTER_EXPANDED',
            'JOURNEY_AI_REGENERATED',
            'JOURNEY_RETURNED',
            'PLAYBOOK_VIEWED',
            'PLAYBOOK_SETUP_STARTED',
            'PLAYBOOK_SETUP_CREATED',
            'PLAYBOOK_SETUP_COMPLETED',
            'PLAYBOOK_TRADE_LINKED',
            'PLAYBOOK_CHECKLIST_REVIEWED',
            'PLAYBOOK_AI_DRAFT_GENERATED',
            'PLAYBOOK_AI_REVIEW_GENERATED',
            'PLAYBOOK_SETUP_VALIDATED',
            'PLAYBOOK_RETURNED',
            'TRADING_MEMORY_VIEWED',
            'TRADING_MEMORY_INDEX_REQUESTED',
            'TRADING_MEMORY_INDEX_SUCCEEDED',
            'TRADING_MEMORY_INDEX_FAILED',
            'TRADING_MEMORY_SEARCH_COMPLETED',
            'TRADING_MEMORY_PATTERN_DETECTED',
            'TRADING_MEMORY_REGENERATED',
            'TRADING_MEMORY_DETERMINISTIC_INSIGHT_VIEWED',
            'TRADING_MEMORY_AI_INSIGHT_VIEWED',
            'SIMILAR_TRADE_OPENED',
            'TRADE_REPLAY_REQUESTED',
            'TRADE_REPLAY_WAITING_FOR_DATA',
            'TRADE_REPLAY_PROVIDER_REQUESTED',
            'TRADE_REPLAY_PROVIDER_FAILED',
            'TRADE_REPLAY_GENERATED',
            'TRADE_REPLAY_VIEWED',
            'TRADE_REPLAY_PLAYED',
            'TRADE_REPLAY_INSIGHT_OPENED',
            'TRADE_REPLAY_REFLECTION_SAVED',
            'TRADE_REPLAY_REGENERATED',
            'TRADE_REPLAY_SIMILAR_PATTERN_FOUND',
            'LEAK_DETECTOR_VIEWED',
            'LEAK_DETECTOR_GENERATED',
            'LEAK_DETECTOR_LEAK_OPENED',
            'LEAK_DETECTOR_RECOVERY_PLAN_VIEWED',
            'LEAK_DETECTOR_RETURNED',
            'TRADE_REVIEW_VIEWED',
            'TRADE_REVIEW_GENERATED',
            'TRADE_REVIEW_ACCEPTED',
            'TRADE_REVIEW_RETURNED',
            'SESSION_REVIEW_VIEWED',
            'SESSION_REVIEW_SELECTED',
            'SESSION_REVIEW_GENERATED',
            'SESSION_REVIEW_EXPANDED',
            'SESSION_REVIEW_ACTION_ITEM_VIEWED',
            'SESSION_REVIEW_RETURNED',
            'UPGRADE_PAGE_VIEWED',
            'LOCKED_PREVIEW_VIEWED',
            'UPGRADE_CTA_CLICKED',
            'BILLING_PAGE_VIEWED',
            'CHECKOUT_STARTED',
            'CHECKOUT_COMPLETED',
            'SUBSCRIPTION_ACTIVATED',
            'SUBSCRIPTION_RENEWED',
            'SUBSCRIPTION_CANCELED',
            'INSUFFICIENT_CREDIT_STATE_VIEWED',
            'BUY_CREDITS_CTA_CLICKED',
            'CREDIT_PACK_SELECTED',
            'CREDITS_USED',
            'USER_SIGNED_UP',
            'TRADING_ACCOUNT_CREATED',
            'CSV_IMPORT_STARTED',
            'CSV_IMPORT_COMPLETED',
            'CSV_IMPORT_FAILED',
            'FIRST_IMPORT_COMPLETED',
            'FIRST_INSIGHT_AVAILABLE',
            'FIRST_INSIGHT_GENERATION_STARTED',
            'FIRST_INSIGHT_GENERATED',
            'SECOND_AI_FEATURE_USED',
            'AI_ALLOWANCE_LOW',
            'PRO_PREVIEW_UNLOCKED',
            'PRO_PREVIEW_VIEWED',
            'PRO_PREVIEW_GENERATION_STARTED',
            'PRO_PREVIEW_GENERATION_SUCCEEDED',
            'PRO_PREVIEW_GENERATION_FAILED',
            'PRO_PREVIEW_CONSUMED',
            'PRO_PREVIEW_USED',
            'PRICING_VIEWED',
            'UPGRADE_PROMPT_VIEWED',
            'PRO_CONVERSION'
        )
    );
