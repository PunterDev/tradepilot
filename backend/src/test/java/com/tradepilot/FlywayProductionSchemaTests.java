package com.tradepilot;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayProductionSchemaTests {
    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V001__fresh_production_schema.sql");
    private static final Path ACTIVATION_MIGRATION = Path.of("src/main/resources/db/migration/V002__first_insight_activation.sql");
    private static final Path CREDIT_PURCHASE_ALIGNMENT_MIGRATION = Path.of("src/main/resources/db/migration/V005__ai_credit_purchase_schema_alignment.sql");
    private static final Path DISTRIBUTED_RATE_LIMIT_MIGRATION = Path.of("src/main/resources/db/migration/V006__distributed_rate_limit_windows.sql");
    private static final Path AI_USAGE_EVENT_SCHEMA_ALIGNMENT_MIGRATION = Path.of("src/main/resources/db/migration/V013__ai_usage_events_schema_alignment.sql");
    private static final Path API_REQUEST_METRICS_ALIGNMENT_MIGRATION = Path.of("src/main/resources/db/migration/V014__api_request_metrics_schema_alignment.sql");
    private static final Path PRODUCTION_SCHEMA_ALIGNMENT_MIGRATION = Path.of("src/main/resources/db/migration/V015__production_schema_alignment.sql");
    private static final Path STRIPE_CHECKOUT_ATTEMPT_BILLING_CYCLE_MIGRATION = Path.of("src/main/resources/db/migration/V016__stripe_checkout_attempt_billing_cycle.sql");
    private static final Path ENABLE_RLS_ON_PUBLIC_TABLES_MIGRATION = Path.of("src/main/resources/db/migration/V017__enable_rls_on_public_tables.sql");
    private static final Path USER_ANALYTICS_EXCLUSION_MIGRATION = Path.of("src/main/resources/db/migration/V018__user_analytics_exclusion.sql");
    private static final Path HISTORICAL_MARKET_DATA_MIGRATION = Path.of("src/main/resources/db/migration/V019__historical_market_data.sql");
    private static final Path BACKTEST_CANONICAL_RESULTS_ALIGNMENT_MIGRATION = Path.of("src/main/resources/db/migration/V022__align_backtest_trade_canonical_results.sql");
    private static final Path VERIFIER = Path.of("db/verify_production_schema.sql");

    @Test
    void freshProductionBaselineIncludesStripeSafetySchema() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("create table ai_credit_purchases");
        assertThat(sql).contains("constraint uk_ai_credit_purchases_checkout_session unique");
        assertThat(sql).contains("ux_ai_credit_purchases_payment_intent_not_null");
        assertThat(sql).contains("ux_users_stripe_customer_id_not_null");
        assertThat(sql).contains("create table processed_stripe_events");
        assertThat(sql).contains("stripe_event_created");
        assertThat(sql).contains("create table stripe_subscription_states");
        assertThat(sql).contains("refund_reversal_credits");
        assertThat(sql).contains("dispute_reversal_credits");
        assertThat(sql).contains("dispute_restored_credits");
        assertThat(sql).doesNotContain("users_purchased_ai_credits_balance_nonnegative");
    }

    @Test
    void activationMigrationIncludesDurablePreviewSchema() throws IOException {
        String sql = Files.readString(ACTIVATION_MIGRATION);

        assertThat(sql).contains("create table user_feature_previews");
        assertThat(sql).contains("uk_user_feature_previews_user_feature");
        assertThat(sql).contains("chk_user_feature_previews_status");
        assertThat(sql).contains("'AVAILABLE'");
        assertThat(sql).contains("'RESERVED'");
        assertThat(sql).contains("'CONSUMED'");
    }

    @Test
    void aiCreditPurchaseAlignmentMigrationBackfillsLegacyNullCounters() throws Exception {
        String databaseName = "credit-purchase-alignment-" + UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")) {
            execute(connection, """
                    create table ai_credit_purchases (
                        id bigint primary key,
                        status varchar(30),
                        refunded_amount_cents integer,
                        reversed_credits integer
                    )
                    """);
            execute(connection, """
                    insert into ai_credit_purchases(id, status, refunded_amount_cents, reversed_credits)
                    values
                        (1, 'COMPLETED', null, null),
                        (2, 'REFUNDED', null, 25),
                        (3, 'DISPUTED', null, 75)
                    """);

            executeStatements(connection, Files.readString(CREDIT_PURCHASE_ALIGNMENT_MIGRATION));

            assertThat(count(connection, """
                    select count(*)
                    from ai_credit_purchases
                    where refunded_amount_cents is null
                       or reversed_credits is null
                       or refund_reversal_credits is null
                       or dispute_reversal_credits is null
                       or dispute_restored_credits is null
                    """)).isZero();
            assertThat(number(connection, "select refund_reversal_credits from ai_credit_purchases where id = 2")).isEqualTo(25);
            assertThat(number(connection, "select dispute_reversal_credits from ai_credit_purchases where id = 3")).isEqualTo(75);
            assertThat(number(connection, "select dispute_restored_credits from ai_credit_purchases where id = 3")).isZero();
            assertThatThrownBy(() -> execute(connection, """
                    insert into ai_credit_purchases(id, status, refunded_amount_cents, reversed_credits, refund_reversal_credits, dispute_reversal_credits, dispute_restored_credits)
                    values (4, 'COMPLETED', 0, 0, 0, 1, 2)
                    """)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection, """
                    insert into ai_credit_purchases(id, status, refunded_amount_cents, reversed_credits, refund_reversal_credits, dispute_reversal_credits, dispute_restored_credits)
                    values (5, 'COMPLETED', null, 0, 0, 0, 0)
                    """)).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void productionVerifierChecksTheSameStripeSafetyObjects() throws IOException {
        String sql = Files.readString(VERIFIER);

        assertThat(sql).contains("ux_users_stripe_customer_id_not_null");
        assertThat(sql).contains("ux_users_stripe_subscription_id_not_null");
        assertThat(sql).contains("uk_ai_credit_purchases_checkout_session");
        assertThat(sql).contains("ux_ai_credit_purchases_payment_intent_not_null");
        assertThat(sql).contains("chk_ai_credit_purchases_refunded_amount_nonnegative");
        assertThat(sql).contains("chk_ai_credit_purchases_reversed_credits_nonnegative");
        assertThat(sql).contains("chk_ai_credit_purchases_dispute_restored_lte_reversal");
        assertThat(sql).contains("user_feature_previews");
        assertThat(sql).contains("uk_user_feature_previews_user_feature");
        assertThat(sql).contains("V005__ai_credit_purchase_schema_alignment");
        assertThat(sql).contains("V006__distributed_rate_limit_windows");
        assertThat(sql).contains("rate_limit_windows");
        assertThat(sql).contains("obsolete_constraint_absent");
    }

    @Test
    void flywayChainIncludesAiCreditPurchaseAlignmentMigration() throws IOException {
        String sql = Files.readString(CREDIT_PURCHASE_ALIGNMENT_MIGRATION);

        assertThat(sql).contains("add column if not exists refunded_amount_cents integer;");
        assertThat(sql).contains("add column if not exists reversed_credits integer;");
        assertThat(sql).contains("add column if not exists refund_reversal_credits integer;");
        assertThat(sql).contains("add column if not exists dispute_reversal_credits integer;");
        assertThat(sql).contains("add column if not exists dispute_restored_credits integer;");
        assertThat(sql).contains("alter column refunded_amount_cents set default 0");
        assertThat(sql).contains("alter column reversed_credits set default 0");
        assertThat(sql).contains("alter column refund_reversal_credits set default 0");
        assertThat(sql).contains("alter column dispute_reversal_credits set default 0");
        assertThat(sql).contains("alter column dispute_restored_credits set default 0");
        assertThat(sql).contains("alter column refunded_amount_cents set not null");
        assertThat(sql).contains("alter column reversed_credits set not null");
        assertThat(sql).contains("alter column refund_reversal_credits set not null");
        assertThat(sql).contains("alter column dispute_reversal_credits set not null");
        assertThat(sql).contains("alter column dispute_restored_credits set not null");
        assertThat(sql).contains("chk_ai_credit_purchases_dispute_restored_lte_reversal");
    }

    @Test
    void distributedRateLimitMigrationCreatesSharedWindowTable() throws IOException {
        String sql = Files.readString(DISTRIBUTED_RATE_LIMIT_MIGRATION);

        assertThat(sql).contains("create table if not exists rate_limit_windows");
        assertThat(sql).contains("rate_key varchar(512) primary key");
        assertThat(sql).contains("request_count integer not null default 0");
        assertThat(sql).contains("chk_rate_limit_windows_request_count_nonnegative");
        assertThat(sql).contains("idx_rate_limit_windows_expires_at");
    }

    @Test
    void aiUsageEventSchemaAlignmentMigrationRestoresJpaCompatibleColumns() throws IOException {
        String sql = Files.readString(AI_USAGE_EVENT_SCHEMA_ALIGNMENT_MIGRATION);

        assertThat(sql).contains("alter table ai_usage_events");
        assertThat(sql).contains("add column if not exists related_resource_id bigint");
        assertThat(sql).contains("alter column usage_type type varchar(40)");
        assertThat(sql).contains("alter column credits_used drop not null");
    }

    @Test
    void apiRequestMetricsAlignmentMigrationRestoresObservabilityColumns() throws IOException {
        String sql = Files.readString(API_REQUEST_METRICS_ALIGNMENT_MIGRATION);

        assertThat(sql).contains("add column if not exists request_id varchar(80)");
        assertThat(sql).contains("add column if not exists error_code varchar(80)");
        assertThat(sql).contains("create index if not exists idx_api_request_metrics_request_id");
    }

    @Test
    void productionSchemaAlignmentMigrationRestoresTheCompleteTradeMemoryContract() throws IOException {
        String sql = Files.readString(PRODUCTION_SCHEMA_ALIGNMENT_MIGRATION);

        assertThat(sql).contains("alter column id type uuid using id::uuid");
        assertThat(sql).contains("alter column vector_point_id type uuid using vector_point_id::uuid");
        assertThat(sql).contains("trade_memories.summary is required by the current JPA mapping");
        assertThat(sql).contains("alter column summary set not null");
        assertThat(sql).contains("timestamp with time zone using %1$I at time zone ''UTC''");
        assertThat(sql).contains("uk_trade_memories_trade_id unique (trade_id)");
        assertThat(sql).contains("uk_trade_memories_vector_point_id unique (vector_point_id)");
        assertThat(sql).contains("auth_tokens.token_hash to VARCHAR(64)");
        assertThat(sql).contains("alter column token_hash type varchar(64)");
        assertThat(sql).contains("add column if not exists trade_date date");
        assertThat(sql).contains("alter column rule_code type varchar(80)");
        assertThat(sql).contains("alter column category type varchar(50)");
        assertThat(sql).contains("alter column severity type varchar(20)");
        assertThat(sql).contains("alter column source type varchar(30)");
        assertThat(sql).contains("idx_performance_milestones_period");
        assertThat(sql).contains("idx_performance_timeline_period");
        assertThat(sql).contains("alter column insight_type type varchar(50)");
        assertThat(sql).contains("invalid value(s) require manual repair");
    }

    @Test
    void stripeCheckoutAttemptBillingCycleMigrationSupportsSafeCheckoutReuse() throws IOException {
        String sql = Files.readString(STRIPE_CHECKOUT_ATTEMPT_BILLING_CYCLE_MIGRATION);

        assertThat(sql).contains("alter table stripe_checkout_attempts");
        assertThat(sql).contains("add column if not exists billing_cycle varchar(10)");
    }

    @Test
    void rowLevelSecurityMigrationExplicitlyEnablesRlsOnEveryPublicTable() throws IOException {
        String sql = Files.readString(ENABLE_RLS_ON_PUBLIC_TABLES_MIGRATION);

        assertThat(sql).contains("from pg_tables");
        assertThat(sql).contains("where schemaname = 'public'");
        assertThat(sql).contains("alter table %I.%I enable row level security");
        assertThat(sql).doesNotContain("force row level security");
        assertThat(sql).doesNotContain("create policy");
        assertThat(sql).doesNotContain("event trigger");
    }

    @Test
    void userAnalyticsExclusionMigrationKeepsExistingUsersIncludedByDefault() throws IOException {
        String sql = Files.readString(USER_ANALYTICS_EXCLUSION_MIGRATION);

        assertThat(sql).contains("alter table users");
        assertThat(sql).contains("add column if not exists analytics_excluded boolean not null default false");
    }

    @Test
    void historicalMarketDataMigrationCreatesCanonicalOneMinuteStorage() throws IOException {
        String sql = Files.readString(HISTORICAL_MARKET_DATA_MIGRATION);

        assertThat(sql).contains("create table if not exists market_bars");
        assertThat(sql).contains("source varchar(32) not null default 'DATABENTO'");
        assertThat(sql).contains("instrument_id bigint not null");
        assertThat(sql).contains("ts_event timestamptz not null");
        assertThat(sql).contains("open numeric(16, 6) not null");
        assertThat(sql).contains("uk_market_bar unique (continuous_symbol, timeframe, ts_event)");
        assertThat(sql).contains("idx_market_bars_lookup");
        assertThat(sql).contains("create table if not exists market_data_import_chunks");
        assertThat(sql).contains("alter table market_bars enable row level security");
        assertThat(sql).contains("alter table market_data_import_chunks enable row level security");
    }

    @Test
    void canonicalBacktestTradesCanPersistWithoutAmbiguousSingleExitFields() throws Exception {
        String databaseName = "backtest-canonical-results-" + UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")) {
            execute(connection, """
                    create table backtest_trades (
                        id bigint primary key,
                        backtest_run_id bigint not null,
                        trade_date date not null,
                        direction varchar(8) not null,
                        entry_time timestamp not null,
                        entry_price numeric(16,6) not null,
                        stop_price numeric(16,6) not null,
                        raw_r numeric(16,6) not null,
                        weighted_r numeric(16,6) not null,
                        outcome varchar(20) not null,
                        exit_model varchar(20) not null,
                        instrument_id bigint not null,
                        setup_metadata_json varchar not null,
                        tp1_price numeric(16,6),
                        runner_target_price numeric(16,6),
                        final_exit_time timestamp,
                        final_exit_price numeric(16,6),
                        model_a_r numeric(16,6),
                        model_b_r numeric(16,6),
                        model_a_weighted_r numeric(16,6),
                        model_b_weighted_r numeric(16,6),
                        ambiguous_intrabar boolean not null default false
                    )
                    """);
            execute(connection, """
                    insert into backtest_trades
                    (id, backtest_run_id, trade_date, direction, entry_time, entry_price, stop_price, raw_r, weighted_r, outcome, exit_model, instrument_id, setup_metadata_json)
                    values (1, 10, date '2026-09-08', 'LONG', timestamp '2026-09-08 14:00:00', 29500.25, 29490.25, 1.0, 0.5, 'WIN', 'MODEL_A', 42004177, '{}')
                    """);

            String migration = Files.readString(BACKTEST_CANONICAL_RESULTS_ALIGNMENT_MIGRATION);
            assertThat(migration).contains("alter column raw_r drop not null");
            assertThat(migration).contains("alter column weighted_r drop not null");
            assertThat(migration).contains("alter column outcome drop not null");
            assertThat(migration).contains("alter column exit_model drop not null");
            assertThat(migration).contains("alter table backtest_runs enable row level security");
            assertThat(migration).contains("alter table backtest_trades enable row level security");
            executeStatements(connection, migration.replaceAll("(?im)^\\s*alter table .* enable row level security;\\s*", ""));

            execute(connection, """
                    insert into backtest_trades
                    (id, backtest_run_id, trade_date, direction, entry_time, entry_price, stop_price, tp1_price, runner_target_price,
                     final_exit_time, final_exit_price, model_a_r, model_b_r, model_a_weighted_r, model_b_weighted_r, instrument_id, setup_metadata_json)
                    values
                    (2, 10, date '2026-09-09', 'LONG', timestamp '2026-09-09 14:12:00', 29547.25, 29530.75, 29555.75, 29560.50,
                     timestamp '2026-09-09 14:30:00', 29558.00, 0.515152, 0.651515, 0.515152, 0.651515, 42004177,
                     '{\"modelAOutcome\":\"WIN\",\"modelBOutcome\":\"WIN\"}')
                    """);

            assertThat(number(connection, "select count(*) from backtest_trades where id = 2 and raw_r is null and weighted_r is null and outcome is null and exit_model is null"))
                    .isEqualTo(1);
            assertThat(number(connection, "select count(*) from backtest_trades where id = 1 and raw_r = 1.0 and weighted_r = 0.5 and outcome = 'WIN' and exit_model = 'MODEL_A'"))
                    .isEqualTo(1);
        }
    }

    private static void executeStatements(Connection connection, String sql) throws SQLException {
        String executableSql = sql.replaceAll("(?m)^\\s*--.*$", "");
        for (String rawStatement : executableSql.split(";")) {
            String statement = rawStatement.trim();
            if (!statement.isBlank()) {
                execute(connection, statement);
            }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    private static int number(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }
}
