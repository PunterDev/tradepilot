-- Keep internal, smoke, and QA accounts out of product-growth analytics.
-- Existing users remain included unless explicitly marked after deployment.
-- To flag the dedicated production smoke account, run separately:
-- update users set analytics_excluded = true where email = '<dedicated smoke account email>';
alter table users
    add column if not exists analytics_excluded boolean not null default false;
