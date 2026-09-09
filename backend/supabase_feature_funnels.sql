-- Feature Funnels & Conversion Analytics telemetry metadata.
-- Safe to run repeatedly in Supabase/PostgreSQL.

alter table feature_usage_events
    add column if not exists action varchar(80),
    add column if not exists event_type varchar(40),
    add column if not exists account_id bigint,
    add column if not exists session_id varchar(120),
    add column if not exists entity_type varchar(80),
    add column if not exists entity_id varchar(120),
    add column if not exists source varchar(80),
    add column if not exists plan_at_event varchar(20),
    add column if not exists metadata_json text,
    add column if not exists occurred_at timestamp;

update feature_usage_events
set occurred_at = created_at
where occurred_at is null;

create index if not exists idx_feature_usage_occurred_at
    on feature_usage_events(occurred_at);

create index if not exists idx_feature_usage_feature_action_created
    on feature_usage_events(feature, action, created_at);

create index if not exists idx_feature_usage_user_feature_action_created
    on feature_usage_events(user_id, feature, action, created_at);

create index if not exists idx_feature_usage_plan_created
    on feature_usage_events(plan_at_event, created_at);

create index if not exists idx_feature_usage_session_created
    on feature_usage_events(session_id, created_at);

create index if not exists idx_feature_usage_entity_created
    on feature_usage_events(entity_type, entity_id, created_at);

comment on column feature_usage_events.action is 'Optional action-level telemetry used by admin funnel analytics. Historical feature-only events may remain null.';
comment on column feature_usage_events.event_type is 'Optional event type at the time of tracking, such as PAGE_VIEW, USER_ACTION, or AI_GENERATION.';
comment on column feature_usage_events.plan_at_event is 'Optional user plan captured at event time for historical funnel segmentation.';
comment on column feature_usage_events.metadata_json is 'Optional sanitized metadata. Do not store private trade notes, AI prompts, or payment details.';

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
            'EXECUTION_ENGINE_VIEWED',
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
            'PRE_ANALYSIS_OPENED',
            'PRE_ANALYSIS_SUBMITTED',
            'PRE_ANALYSIS_GENERATED',
            'PRE_ANALYSIS_FAILED',
            'PRE_ANALYSIS_NO_TRADE',
            'PRE_ANALYSIS_WAIT',
            'PRE_ANALYSIS_ENTRY_VALID',
            'PRE_ANALYSIS_INVALIDATED',
            'PRE_ANALYSIS_FEEDBACK_SUBMITTED',
            'PRE_ANALYSIS_PLAYBOOK_UPDATED',
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
