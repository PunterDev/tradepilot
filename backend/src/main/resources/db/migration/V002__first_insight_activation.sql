-- First Insight Activation production schema additions.
-- Additive after V001 fresh production baseline.

create table user_feature_previews (
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

create index idx_user_feature_previews_user_id on user_feature_previews(user_id);
create index idx_user_feature_previews_status on user_feature_previews(status);
create index idx_user_feature_previews_user_status on user_feature_previews(user_id, status);
