-- Hardening for SEO acquisition analytics.
-- Safe for existing rows: event_id is nullable and only unique when present.

alter table seo_acquisition_events
    add column if not exists event_id varchar(36);

alter table seo_acquisition_events
    alter column anonymous_id type varchar(100);

alter table user_acquisition_attribution
    alter column anonymous_id type varchar(100);

create unique index if not exists ux_seo_acq_events_event_id_not_null
    on seo_acquisition_events(event_id)
    where event_id is not null and event_id <> '';
