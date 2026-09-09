-- SEO acquisition analytics.
-- Stores privacy-safe acquisition events and first-touch attribution only.

create table seo_acquisition_events (
    id bigserial primary key,
    anonymous_id varchar(80),
    event_name varchar(80) not null,
    landing_page varchar(220) not null,
    tool varchar(80),
    cta_label varchar(120),
    cta_destination varchar(220),
    referrer varchar(500),
    utm_source varchar(120),
    utm_medium varchar(120),
    utm_campaign varchar(160),
    utm_content varchar(160),
    utm_term varchar(160),
    occurred_at timestamp not null default now(),
    created_at timestamp not null default now()
);

create index idx_seo_acq_events_occurred_at on seo_acquisition_events(occurred_at);
create index idx_seo_acq_events_event_occurred on seo_acquisition_events(event_name, occurred_at);
create index idx_seo_acq_events_landing_occurred on seo_acquisition_events(landing_page, occurred_at);
create index idx_seo_acq_events_tool_occurred on seo_acquisition_events(tool, occurred_at);
create index idx_seo_acq_events_anonymous on seo_acquisition_events(anonymous_id);
create index idx_seo_acq_events_source_medium on seo_acquisition_events(utm_source, utm_medium, occurred_at);
create index idx_seo_acq_events_campaign on seo_acquisition_events(utm_campaign, occurred_at);

create table user_acquisition_attribution (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    anonymous_id varchar(80),
    landing_page varchar(220) not null,
    initial_referrer varchar(500),
    utm_source varchar(120),
    utm_medium varchar(120),
    utm_campaign varchar(160),
    utm_content varchar(160),
    utm_term varchar(160),
    first_seen_at timestamp,
    signup_completed_at timestamp not null default now(),
    first_import_completed_at timestamp,
    first_insight_generated_at timestamp,
    pro_converted_at timestamp,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_user_acquisition_attribution_user unique (user_id)
);

create index idx_user_acq_landing on user_acquisition_attribution(landing_page);
create index idx_user_acq_signup_completed on user_acquisition_attribution(signup_completed_at);
create index idx_user_acq_first_insight on user_acquisition_attribution(first_insight_generated_at);
create index idx_user_acq_pro_converted on user_acquisition_attribution(pro_converted_at);
create index idx_user_acq_source_medium on user_acquisition_attribution(utm_source, utm_medium);
create index idx_user_acq_campaign on user_acquisition_attribution(utm_campaign);
