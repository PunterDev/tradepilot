# Production Database Bootstrap

This document describes the deterministic path for creating a brand-new TradePilot production PostgreSQL/Supabase database with Flyway. It is for an empty database only; do not run the Flyway baseline on an existing production database that was created with the historical `supabase_*.sql` scripts.

## Current Schema Source Of Truth

For a new empty production database, the canonical schema is:

1. `backend/src/main/resources/db/migration/V001__fresh_production_schema.sql`
2. `backend/src/main/resources/db/migration/V002__first_insight_activation.sql`
3. `backend/src/main/resources/db/migration/V003__seo_acquisition_analytics.sql`
4. `backend/src/main/resources/db/migration/V004__seo_analytics_hardening.sql`
5. `backend/src/main/resources/db/migration/V005__ai_credit_purchase_schema_alignment.sql`
6. `backend/src/main/resources/db/migration/V006__distributed_rate_limit_windows.sql`

Spring Boot now runs Flyway before Hibernate starts. Production must run with:

- `FLYWAY_ENABLED=true`
- `JPA_DDL_AUTO=validate`
- PostgreSQL driver and PostgreSQL/Supabase connection settings

If Flyway migration fails, application startup fails. If Hibernate validation fails after migration, application startup fails.

## Historical SQL Review

The legacy `backend/supabase_*.sql` files are preserved as historical references and existing-database repair scripts. They are superseded for brand-new databases by `V001__fresh_production_schema.sql`.

Superseded base/schema scripts:

- `backend/supabase_schema_consistency_fixes.sql`
- `backend/supabase_user_isolation_indexes.sql`
- `backend/supabase_billing_limits.sql`
- `backend/supabase_ai_credit_usage.sql`
- `backend/supabase_ai_credit_packs.sql`
- `backend/supabase_admin_portal.sql`
- `backend/supabase_contact_messages.sql`
- `backend/supabase_user_token_version.sql`

Superseded feature schema scripts:

- `backend/supabase_feature_usage.sql`
- `backend/supabase_feature_usage_events_add_ai_leak_detector.sql`
- `backend/supabase_feature_usage_events_add_performance_timeline.sql`
- `backend/supabase_feature_usage_events_add_trading_memory_actions.sql`
- `backend/supabase_ai_usage_events_add_leak_detector.sql`
- `backend/supabase_ai_leak_detector_snapshots.sql`
- `backend/supabase_trading_dna_performance.sql`
- `backend/supabase_performance_timeline.sql`
- `backend/supabase_playbook.sql`
- `backend/supabase_trade_replay_v3.sql`
- `backend/supabase_trade_replay_enum_constraint_sync.sql`
- `backend/supabase_trading_memory.sql`
- `backend/supabase_trading_memory_hardening.sql`
- `backend/supabase_session_reviews.sql`
- `backend/supabase_trade_contract_symbols.sql`
- `backend/supabase_trade_journal_metadata.sql`
- `backend/supabase_trade_session_unknown.sql`
- `backend/supabase_account_r_multiple_support.sql`
- `backend/supabase_performance_indexes.sql`
- `backend/supabase_observability.sql`
- `backend/supabase_observability_request_diagnostics.sql`

Superseded Stripe production-safety scripts:

- `backend/supabase_stripe_production_safety.sql`
- `backend/supabase_stripe_live_blockers.sql`
- `backend/supabase_stripe_reversal_reason_accounting.sql`
- `backend/supabase_stripe_allow_negative_purchased_credit_balance.sql`

Read-only historical verification script:

- `backend/supabase_performance_index_verification.sql`

## Historical Script Conflicts Avoided

The Flyway baseline intentionally does not carry forward obsolete historical states:

- `users_purchased_ai_credits_balance_nonnegative` is not created. Refund/dispute reversal policy can make purchased credits negative when credits were consumed before reversal.
- Old enum/check constraints that were repeatedly dropped and recreated are collapsed into the current accepted values.
- Stripe event ordering, subscription state, checkout attempt, credit purchase ledger, PaymentIntent/Charge uniqueness, and reason-aware refund/dispute columns are included directly or aligned by Flyway follow-up migrations.
- Production shared rate-limit state is added by `V006__distributed_rate_limit_windows.sql`.
- Historical correction scripts that add columns after table creation are flattened into the baseline.

## Dependency Order

The baseline creates objects in this dependency order:

1. `users`
2. `trading_accounts`, `import_batches`, `trades`
3. journal, AI review, discipline, admin, auth, contact tables
4. billing and Stripe tables
5. feature usage and observability tables
6. AI cache/snapshot tables
7. playbook, replay, and trade memory tables
8. indexes, unique constraints, partial unique indexes, and check constraints needed by the current application

## Bootstrap Procedure

Before creating or bootstrapping the real production database, the latest `postgres-flyway-baseline` CI job must pass with:

- 1 run
- 0 failures
- 0 errors
- 0 skipped

This CI job runs the Testcontainers PostgreSQL baseline verification against `postgres:16-alpine`. A skipped result is a failed production gate because it means the empty-database Flyway path was not actually proven.

1. Create an empty Supabase production project.
2. Obtain the PostgreSQL connection settings for the migration/admin connection.
3. Configure a migration-capable environment with:
   - `DB_URL` or `DATABASE_URL`
   - `DB_USERNAME` or `DATABASE_USERNAME`
   - `DB_PASSWORD` or `DATABASE_PASSWORD`
   - `DB_DRIVER=org.postgresql.Driver`
   - `FLYWAY_ENABLED=true`
   - `JPA_DDL_AUTO=validate`
4. From `backend/`, run Flyway through Spring Boot startup or a controlled migration command against the empty database.
5. Verify `flyway_schema_history` contains version `001`, description `fresh production schema`, and `success=true`.
6. Run the read-only schema verifier:
   ```sql
   \i backend/db/verify_production_schema.sql
   ```
7. Confirm every verifier row returns `OK`.
8. Confirm no production seed/config data is missing. The current baseline does not insert user/application seed rows.
9. Run DB integrity checks from `docs/STRIPE_PROD_DB_PREFLIGHT.md` if importing historical data.
10. Connect the Render production backend with `FLYWAY_ENABLED=true` and `JPA_DDL_AUTO=validate`.

## Empty Database Verification

Preferred staging validation:

1. Create a disposable empty Supabase/PostgreSQL database.
2. Configure the backend with the disposable database URL and production-like values for non-secret placeholders.
3. Start the backend with `FLYWAY_ENABLED=true` and `JPA_DDL_AUTO=validate`.
4. Confirm startup succeeds, Flyway version `001` is installed, and Hibernate validation passes.
5. Run `backend/db/verify_production_schema.sql`.

The default `test` profile uses H2 with Hibernate `create-drop`; Flyway is disabled there so the existing unit/integration test suite remains fast and isolated.

When Docker is available locally, run the same production-readiness gate from `backend/`:

```bash
mvn -Ppostgres-integration test -Dtest=FlywayPostgresBaselineIntegrationTest
```

The production gate is valid only when the Surefire report for `FlywayPostgresBaselineIntegrationTest` shows:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

If Docker is unavailable, ordinary local `mvn test` may skip this Testcontainers check. That skipped local result is developer-friendly only; it is not acceptable for production database bootstrap approval.

## One-Time Legacy Local Database Adoption

This section is only for a local PostgreSQL database that already existed before Flyway became the schema owner and now fails with:

```text
Found non-empty schema(s) "public" but no schema history table.
```

Do not enable `baselineOnMigrate=true` globally and do not baseline at the latest version. The baseline version must be the newest migration that is already materially reflected in the existing database. The next migration after the baseline must still run normally.

For the current legacy-local failure mode, choose baseline version `004` only if the metadata checks below prove that V001 through V004 are already represented and V005 is not fully represented. This keeps `V005__ai_credit_purchase_schema_alignment.sql` pending so Flyway applies the data-safe `ai_credit_purchases` null backfill, `NOT NULL` alignment, and accounting constraints.

Run these read-only checks against the local database before baselining:

```sql
select to_regclass('public.flyway_schema_history') as flyway_history_table;

select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in (
      'users',
      'ai_credit_purchases',
      'processed_stripe_events',
      'stripe_subscription_states',
      'stripe_checkout_attempts',
      'user_feature_previews',
      'seo_acquisition_events',
      'user_acquisition_attribution'
  )
order by table_name;

select column_name, data_type, character_maximum_length, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name = 'seo_acquisition_events'
  and column_name in ('anonymous_id', 'event_id')
order by column_name;

select indexname
from pg_indexes
where schemaname = 'public'
  and indexname in (
      'ux_seo_acq_events_event_id_not_null',
      'ux_users_stripe_customer_id_not_null',
      'ux_users_stripe_subscription_id_not_null',
      'uk_ai_credit_purchases_checkout_session',
      'ux_ai_credit_purchases_payment_intent_not_null',
      'ux_ai_credit_purchases_charge_not_null'
  )
order by indexname;

select column_name, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name = 'ai_credit_purchases'
  and column_name in (
      'refunded_amount_cents',
      'reversed_credits',
      'refund_reversal_credits',
      'dispute_reversal_credits',
      'dispute_restored_credits'
  )
order by column_name;

select conname
from pg_constraint
where connamespace = 'public'::regnamespace
  and conname in (
      'chk_ai_credit_purchases_refunded_amount_nonnegative',
      'chk_ai_credit_purchases_reversed_credits_nonnegative',
      'chk_ai_credit_purchases_refund_reversal_nonnegative',
      'chk_ai_credit_purchases_dispute_reversal_nonnegative',
      'chk_ai_credit_purchases_dispute_restored_nonnegative',
      'chk_ai_credit_purchases_dispute_restored_lte_reversal'
  )
order by conname;

select
    count(*) filter (where refunded_amount_cents is null) as refunded_amount_nulls,
    count(*) filter (where reversed_credits is null) as reversed_credits_nulls,
    count(*) filter (where refund_reversal_credits is null) as refund_reversal_nulls,
    count(*) filter (where dispute_reversal_credits is null) as dispute_reversal_nulls,
    count(*) filter (where dispute_restored_credits is null) as dispute_restored_nulls
from ai_credit_purchases;
```

If V001 through V004 are present and the V005 `ai_credit_purchases` checks are missing or nullable, inspect the pending Flyway state from the repository root:

```bash
backend/scripts/flyway-dev.sh flyway:info
```

The helper reads `backend/src/main/resources/application-dev.yml`, resolves the same `spring.datasource` values the dev Spring profile uses, writes them to a restrictive temporary Flyway config file, and deletes that file on exit. It does not print the password or require copying the password into shell history. `application-dev.yml` remains local-only and ignored by Git.

Run the explicit baseline from the repository root:

```bash
backend/scripts/flyway-dev.sh \
  flyway:baseline \
  -Dflyway.baselineVersion=004 \
  -Dflyway.baselineDescription="legacy schema through V004"
```

Then run the pending migration:

```bash
backend/scripts/flyway-dev.sh flyway:migrate
```

Verify that V005 actually ran:

```sql
select installed_rank, version, description, type, success
from flyway_schema_history
order by installed_rank;

select column_name, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name = 'ai_credit_purchases'
  and column_name in (
      'refunded_amount_cents',
      'reversed_credits',
      'refund_reversal_credits',
      'dispute_reversal_credits',
      'dispute_restored_credits'
  )
order by column_name;

select conname
from pg_constraint
where connamespace = 'public'::regnamespace
  and conname in (
      'chk_ai_credit_purchases_refunded_amount_nonnegative',
      'chk_ai_credit_purchases_reversed_credits_nonnegative',
      'chk_ai_credit_purchases_refund_reversal_nonnegative',
      'chk_ai_credit_purchases_dispute_reversal_nonnegative',
      'chk_ai_credit_purchases_dispute_restored_nonnegative',
      'chk_ai_credit_purchases_dispute_restored_lte_reversal'
  )
order by conname;
```

After the baseline and migration, start the backend with Flyway enabled and Hibernate validation:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The dev profile must use `FLYWAY_ENABLED=true` and `JPA_DDL_AUTO=validate` or equivalent local overrides. Startup must complete without Hibernate attempting schema mutation. If any V001-V004 metadata check fails, do not use baseline version `004`; either repair the legacy schema first or baseline to the last version that is genuinely present, then let Flyway run every later migration.

## Production Deployment Gate

Require the GitHub Actions status check named `postgres-flyway-baseline` before any production backend deployment that can run against a new production database. If branch protection or Render deployment gates are managed outside this repository, configure them manually so the production branch cannot deploy unless the latest commit has a passing `postgres-flyway-baseline` job. Keep the ordinary `backend-tests` job required as well, but do not treat it as a substitute for the PostgreSQL baseline gate because local-friendly skips are allowed there.

## Post-Migration Verification Queries

Run:

```sql
select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;

\i backend/db/verify_production_schema.sql
```

Key Stripe production checks:

```sql
select indexname, indexdef
from pg_indexes
where schemaname = 'public'
  and indexname in (
      'ux_users_stripe_customer_id_not_null',
      'ux_users_stripe_subscription_id_not_null',
      'ux_ai_credit_purchases_payment_intent_not_null',
      'ux_ai_credit_purchases_charge_not_null'
  )
order by indexname;

select conname
from pg_constraint
where connamespace = 'public'::regnamespace
  and conname in (
      'uk_ai_credit_purchases_checkout_session',
      'chk_ai_credit_purchases_dispute_restored_lte_reversal'
  );
```

## Rollback And Recovery

If migration fails on an empty database:

- Do not connect the production app to the partially migrated database.
- Inspect `flyway_schema_history` for the failed version.
- If the database is truly empty and not yet public, the safest recovery is to drop and recreate the Supabase project or reset the schema, then rerun Flyway from a clean state.
- If any customer data exists, stop and create a recovery plan before dropping objects.

If backend startup fails after migration:

- Keep billing disabled from the public frontend.
- Preserve the database for inspection.
- Review startup logs for Flyway or Hibernate validation errors.
- Fix the migration in a new Flyway version if the failed database must be preserved. Do not edit a migration that has already succeeded in any shared environment.

If verification finds an unsafe obsolete constraint:

- Confirm `users_purchased_ai_credits_balance_nonnegative` is absent before enabling refund/dispute webhooks.
- If present, do not enable live billing until a forward migration drops it.

## Remaining Operational Notes

- Existing production/historical databases should not be converted by pretending this baseline already ran. Use a separate existing-database migration plan or Flyway baseline-on-migrate strategy only after a deliberate data audit.
- Stripe Live products, prices, webhook endpoint, Render environment variables, and real-payment smoke tests remain separate rollout gates.
