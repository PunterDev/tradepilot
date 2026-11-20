package com.tradepilot;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayPostgresBaselineIntegrationTest {
    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String REQUIRE_DOCKER_PROPERTY = "tradepilot.postgres-baseline.require-docker";

    @Test
    void emptyPostgresDatabaseMigratesAndValidatesProductionBaseline() throws Exception {
        boolean requireDocker = Boolean.getBoolean(REQUIRE_DOCKER_PROPERTY);

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)) {
            try {
                postgres.start();
            } catch (RuntimeException ex) {
                if (requireDocker) {
                    throw new AssertionError("Docker/Testcontainers is required for the PostgreSQL Flyway baseline CI gate.", ex);
                }
                Assumptions.assumeTrue(false, "Docker/Testcontainers unavailable; skipping optional local PostgreSQL baseline verification.");
            }

            migrateAndValidate(postgres);
        }
    }

    private static void migrateAndValidate(PostgreSQLContainer<?> postgres) throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("filesystem:src/main/resources/db/migration")
                .configuration(java.util.Map.of("flyway.postgresql.transactional.lock", "false"))
                .load();

        try (Connection connection = connect(postgres)) {
            assertApplicationTablesAreAbsent(connection);
        }

        MigrateResult firstMigration = flyway.migrate();
        assertThat(firstMigration.migrationsExecuted).isEqualTo(31);

        try (Connection connection = connect(postgres)) {
            assertThat(scalar(connection, "select version()")).contains("PostgreSQL 16");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '001' and success = true"))
                    .isEqualTo("001");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '002' and success = true"))
                    .isEqualTo("002");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '003' and success = true"))
                    .isEqualTo("003");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '004' and success = true"))
                    .isEqualTo("004");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '005' and success = true"))
                    .isEqualTo("005");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '006' and success = true"))
                    .isEqualTo("006");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '007' and success = true"))
                    .isEqualTo("007");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '008' and success = true"))
                    .isEqualTo("008");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '009' and success = true"))
                    .isEqualTo("009");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '010' and success = true"))
                    .isEqualTo("010");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '011' and success = true"))
                    .isEqualTo("011");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '012' and success = true"))
                    .isEqualTo("012");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '013' and success = true"))
                    .isEqualTo("013");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '014' and success = true"))
                    .isEqualTo("014");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '015' and success = true"))
                    .isEqualTo("015");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '016' and success = true"))
                    .isEqualTo("016");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '017' and success = true"))
                    .isEqualTo("017");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '018' and success = true"))
                    .isEqualTo("018");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '019' and success = true"))
                    .isEqualTo("019");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '020' and success = true"))
                    .isEqualTo("020");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '021' and success = true"))
                    .isEqualTo("021");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '022' and success = true"))
                    .isEqualTo("022");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '023' and success = true"))
                    .isEqualTo("023");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '024' and success = true")).isEqualTo("024");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '025' and success = true")).isEqualTo("025");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '026' and success = true")).isEqualTo("026");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '027' and success = true")).isEqualTo("027");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '028' and success = true")).isEqualTo("028");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '029' and success = true")).isEqualTo("029");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '030' and success = true")).isEqualTo("030");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '032' and success = true")).isEqualTo("032");

            assertRequiredTables(connection);
            assertAiUsageEventsSchema(connection);
            assertApiRequestMetricsSchema(connection);
            assertTradeMemoriesSchema(connection);
            assertOtherV015SchemaAlignments(connection);
            assertStripeCheckoutAttemptBillingCycle(connection);
            assertUserAnalyticsExclusion(connection);
            assertHistoricalMarketDataSchema(connection);
            assertHistoricalMarketDataDuplicateInsertIsIdempotent(connection);
            assertCanonicalBacktestTradeSchema(connection);
            assertAccountSurvivalSchema(connection);
            assertStripeIndexesAndConstraints(connection);
            assertNegativePurchasedCreditsAreAllowed(connection);
            assertVerifierScriptReportsOk(connection);
            assertRowLevelSecurityEnabled(connection);
            assertNonBypassRoleCannotReadUsers(connection);
        }

        assertHibernateValidatesMigratedSchema(postgres);

        MigrateResult secondMigration = flyway.migrate();
        assertThat(secondMigration.migrationsExecuted).isZero();

        try (Connection connection = connect(postgres)) {
            assertThat(count(connection, "select count(*) from flyway_schema_history where version = '001' and success = true"))
                    .isEqualTo(1);
        }
    }

    private static Connection connect(PostgreSQLContainer<?> postgres) throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static void assertRequiredTables(Connection connection) throws SQLException {
        List<String> tables = List.of(
                "users",
                "ai_credit_purchases",
                "processed_stripe_events",
                "stripe_subscription_states",
                "stripe_checkout_attempts",
                "user_feature_previews",
                "seo_acquisition_events",
                "user_acquisition_attribution",
                "rate_limit_windows",
                "market_bars",
                "market_data_import_chunks",
                "manual_backtest_sessions",
                "manual_backtest_trades",
                "manual_backtest_trade_events"
                ,"account_risk_profiles", "next_session_protection_commitments", "next_session_protection_evaluations", "next_session_protection_rule_results"
        );
        for (String table : tables) {
            assertThat(count(connection, """
                    select count(*)
                    from information_schema.tables
                    where table_schema = 'public'
                      and table_name = '%s'
                    """.formatted(table))).as("table " + table).isEqualTo(1);
        }
    }

    private static void assertAccountSurvivalSchema(Connection connection) throws SQLException {
        assertThat(count(connection, "select count(*) from information_schema.table_constraints where table_name = 'next_session_protection_evaluations' and constraint_name = 'uk_protection_evaluation_fingerprint'"))
                .isEqualTo(1);
        assertThat(count(connection, "select count(*) from information_schema.table_constraints where table_name = 'next_session_protection_rule_results' and constraint_name = 'uk_protection_rule_result'"))
                .isEqualTo(1);
        assertVarcharColumn(connection, "next_session_protection_evaluations", "source_fingerprint", 128, "NO");
        assertThat(scalar(connection, "select data_type from information_schema.columns where table_name = 'next_session_protection_rule_results' and column_name = 'evidence'"))
                .isEqualTo("text");
    }

    private static void assertApplicationTablesAreAbsent(Connection connection) throws SQLException {
        assertThat(count(connection, """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name not in ('pg_stat_statements')
                """)).isZero();
    }

    private static void assertStripeIndexesAndConstraints(Connection connection) throws SQLException {
        assertThat(indexDefinition(connection, "ux_users_stripe_customer_id_not_null"))
                .contains("UNIQUE")
                .contains("stripe_customer_id")
                .contains("WHERE");
        assertThat(indexDefinition(connection, "ux_users_stripe_subscription_id_not_null"))
                .contains("UNIQUE")
                .contains("stripe_subscription_id")
                .contains("WHERE");
        assertThat(indexDefinition(connection, "ux_ai_credit_purchases_payment_intent_not_null"))
                .contains("UNIQUE")
                .contains("stripe_payment_intent_id")
                .contains("WHERE");

        long userId = insertUser(connection, "postgres-flyway-constraints@example.com");
        insertUser(connection, "postgres-flyway-null-customer-1@example.com");
        insertUser(connection, "postgres-flyway-null-customer-2@example.com");
        insertUserWithStripeIds(connection, "postgres-flyway-blank-stripe-1@example.com", "", "");
        insertUserWithStripeIds(connection, "postgres-flyway-blank-stripe-2@example.com", "", "");
        insertUserWithStripeIds(connection, "postgres-flyway-real-stripe-1@example.com", "cus_real_unique", "sub_real_unique");

        assertThatThrownBy(() -> insertUserWithStripeIds(connection, "postgres-flyway-real-stripe-2@example.com", "cus_real_unique", "sub_other"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insertUserWithStripeIds(connection, "postgres-flyway-real-stripe-3@example.com", "cus_other", "sub_real_unique"))
                .isInstanceOf(SQLException.class);

        insertCreditPurchase(connection, userId, "cs_unique", "pi_unique", "ch_unique");
        assertThatThrownBy(() -> insertCreditPurchase(connection, userId, "cs_unique", "pi_other", "ch_other"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insertCreditPurchase(connection, userId, "cs_other", "pi_unique", "ch_other_2"))
                .isInstanceOf(SQLException.class);

        execute(connection, "insert into processed_stripe_events(event_id, event_type, stripe_event_created) values ('evt_unique', 'checkout.session.completed', now())");
        assertThatThrownBy(() -> execute(connection, "insert into processed_stripe_events(event_id, event_type, stripe_event_created) values ('evt_unique', 'invoice.paid', now())"))
                .isInstanceOf(SQLException.class);

        execute(connection, "insert into stripe_subscription_states(subscription_id, last_event_id, last_event_type, last_event_created) values ('sub_state_unique', 'evt_state', 'customer.subscription.updated', now())");
        assertThatThrownBy(() -> execute(connection, "insert into stripe_subscription_states(subscription_id, last_event_id, last_event_type, last_event_created) values ('sub_state_unique', 'evt_state_2', 'customer.subscription.deleted', now())"))
                .isInstanceOf(SQLException.class);

        execute(connection, "insert into user_feature_previews(user_id, feature_key) values (%d, 'WEEKLY_COACH_PRO_PREVIEW')".formatted(userId));
        assertThatThrownBy(() -> execute(connection, "insert into user_feature_previews(user_id, feature_key) values (%d, 'WEEKLY_COACH_PRO_PREVIEW')".formatted(userId)))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> insertCreditPurchaseWithReversalCounts(connection, userId, "cs_bad_reversal", "pi_bad_reversal", 10, 5))
                .isInstanceOf(SQLException.class);

        assertThat(count(connection, """
                select count(*)
                from pg_constraint
                where connamespace = 'public'::regnamespace
                  and conname = 'users_purchased_ai_credits_balance_nonnegative'
                """)).isZero();
        assertThatThrownBy(() -> execute(connection, "insert into rate_limit_windows(rate_key, request_count, window_start, expires_at) values ('bad', -1, now(), now())"))
                .isInstanceOf(SQLException.class);
    }

    private static void assertAiUsageEventsSchema(Connection connection) throws SQLException {
        assertThat(scalar(connection, """
                select data_type
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'ai_usage_events'
                  and column_name = 'related_resource_id'
                """))
                .isEqualTo("bigint");
        assertThat(scalar(connection, """
                select is_nullable
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'ai_usage_events'
                  and column_name = 'related_resource_id'
                """))
                .isEqualTo("YES");
        assertThat(scalar(connection, """
                select character_maximum_length::text
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'ai_usage_events'
                  and column_name = 'usage_type'
                """))
                .isEqualTo("40");
        assertThat(scalar(connection, """
                select is_nullable
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'ai_usage_events'
                  and column_name = 'credits_used'
                """))
                .isEqualTo("YES");
    }

    private static void assertApiRequestMetricsSchema(Connection connection) throws SQLException {
        assertNullableVarcharColumn(connection, "api_request_metrics", "request_id", 80);
        assertNullableVarcharColumn(connection, "api_request_metrics", "error_code", 80);
        for (String column : List.of("input_tokens", "cached_input_tokens", "output_tokens", "reasoning_tokens", "retry_attempt")) {
            assertThat(scalar(connection, """
                    select data_type
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'ai_request_metrics'
                      and column_name = '%s'
                    """.formatted(column))).as("ai_request_metrics.%s", column).isEqualTo("integer");
        }
        assertNullableVarcharColumn(connection, "ai_request_metrics", "error_category", 40);
        assertThat(indexDefinition(connection, "idx_api_request_metrics_request_id"))
                .contains("request_id");
    }

    private static void assertTradeMemoriesSchema(Connection connection) throws SQLException {
        assertThat(scalar(connection, """
                select data_type
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'trade_memories'
                  and column_name = 'id'
                """)).isEqualTo("uuid");
        assertThat(scalar(connection, """
                select data_type
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'trade_memories'
                  and column_name = 'vector_point_id'
                """)).isEqualTo("uuid");
        assertThat(scalar(connection, """
                select data_type
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'trade_memories'
                  and column_name = 'summary'
                """)).isEqualTo("text");
        assertThat(scalar(connection, """
                select is_nullable
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'trade_memories'
                  and column_name = 'summary'
                """)).isEqualTo("NO");
        for (String column : List.of("last_attempted_at", "next_retry_at", "created_at", "updated_at", "embedded_at")) {
            assertThat(scalar(connection, """
                    select data_type
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'trade_memories'
                      and column_name = '%s'
                    """.formatted(column))).isEqualTo("timestamp with time zone");
        }
        assertThat(count(connection, """
                select count(*)
                from pg_constraint
                where conrelid = 'trade_memories'::regclass
                  and conname in ('uk_trade_memories_trade_id', 'uk_trade_memories_vector_point_id')
                """)).isEqualTo(2);
    }

    private static void assertOtherV015SchemaAlignments(Connection connection) throws SQLException {
        assertVarcharColumn(connection, "auth_tokens", "token_hash", 64, "NO");
        assertVarcharColumn(connection, "trade_rule_violations", "rule_code", 80, "NO");
        assertVarcharColumn(connection, "trade_rule_violations", "category", 50, "NO");
        assertVarcharColumn(connection, "trade_rule_violations", "severity", 20, "NO");
        assertVarcharColumn(connection, "trade_rule_violations", "source", 30, "NO");
        assertThat(scalar(connection, """
                select data_type
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'trade_rule_violations'
                  and column_name = 'trade_date'
                """)).isEqualTo("date");
        assertThat(indexDefinition(connection, "idx_performance_milestones_period")).contains("account_id").contains("period_start");
        assertThat(indexDefinition(connection, "idx_performance_timeline_period")).contains("account_id").contains("period_start");
        assertVarcharColumn(connection, "trade_replay_insights", "insight_type", 50, "NO");
    }

    private static void assertStripeCheckoutAttemptBillingCycle(Connection connection) throws SQLException {
        assertNullableVarcharColumn(connection, "stripe_checkout_attempts", "billing_cycle", 10);
    }

    private static void assertUserAnalyticsExclusion(Connection connection) throws SQLException {
        assertThat(scalar(connection, """
                select data_type
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'users'
                  and column_name = 'analytics_excluded'
                """)).isEqualTo("boolean");
        assertThat(scalar(connection, """
                select is_nullable
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'users'
                  and column_name = 'analytics_excluded'
                """)).isEqualTo("NO");
        assertThat(scalar(connection, """
                select column_default
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'users'
                  and column_name = 'analytics_excluded'
                """)).contains("false");
    }

    private static void assertHistoricalMarketDataSchema(Connection connection) throws SQLException {
        assertVarcharColumn(connection, "market_bars", "symbol", 10, "NO");
        assertVarcharColumn(connection, "market_bars", "continuous_symbol", 20, "NO");
        assertVarcharColumn(connection, "market_bars", "source", 32, "NO");
        assertVarcharColumn(connection, "market_bars", "timeframe", 10, "NO");
        assertThat(scalar(connection, """
                select data_type from information_schema.columns
                where table_schema = 'public' and table_name = 'market_bars' and column_name = 'ts_event'
                """)).isEqualTo("timestamp with time zone");
        assertThat(indexDefinition(connection, "idx_market_bars_lookup")).contains("continuous_symbol").contains("timeframe").contains("ts_event");
        assertThat(indexDefinition(connection, "idx_market_bars_instrument")).contains("instrument_id").contains("ts_event");
        assertThat(indexDefinition(connection, "idx_market_data_import_chunks_lookup")).contains("continuous_symbol").contains("timeframe");
    }

    private static void assertHistoricalMarketDataDuplicateInsertIsIdempotent(Connection connection) throws SQLException {
        String insert = """
                insert into market_bars
                (symbol, continuous_symbol, instrument_id, timeframe, ts_event, open, high, low, close, volume)
                values ('NQ', 'NQ.v.0', 123456, '1m', '2026-01-02T14:30:00Z', 21000.25, 21001, 20999.75, 21000.50, 10)
                on conflict (continuous_symbol, timeframe, ts_event) do nothing
                """;
        try (Statement statement = connection.createStatement()) {
            assertThat(statement.executeUpdate(insert)).isEqualTo(1);
            assertThat(statement.executeUpdate(insert)).isZero();
        }
        assertThat(count(connection, "select count(*) from market_bars where continuous_symbol = 'NQ.v.0' and timeframe = '1m'"))
                .isEqualTo(1);
        assertThat(scalar(connection, "select source from market_bars where continuous_symbol = 'NQ.v.0' and timeframe = '1m'"))
                .isEqualTo("DATABENTO");
    }

    private static void assertCanonicalBacktestTradeSchema(Connection connection) throws SQLException {
        for (String column : List.of("raw_r", "weighted_r", "outcome", "exit_model", "exit_time", "exit_price")) {
            assertThat(scalar(connection, """
                    select is_nullable
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'backtest_trades'
                      and column_name = '%s'
                    """.formatted(column))).isEqualTo("YES");
        }
        for (String column : List.of("model_a_r", "model_b_r", "model_a_weighted_r", "model_b_weighted_r")) {
            assertThat(scalar(connection, """
                    select data_type
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'backtest_trades'
                      and column_name = '%s'
                    """.formatted(column))).isEqualTo("numeric");
        }
    }

    private static void assertRowLevelSecurityEnabled(Connection connection) throws SQLException {
        long publicTableCount = count(connection, """
                select count(*)
                from pg_class c
                join pg_namespace n on n.oid = c.relnamespace
                where n.nspname = 'public'
                  and c.relkind in ('r', 'p')
                """);
        long rlsEnabledTableCount = count(connection, """
                select count(*)
                from pg_class c
                join pg_namespace n on n.oid = c.relnamespace
                where n.nspname = 'public'
                  and c.relkind in ('r', 'p')
                  and c.relrowsecurity
                """);

        assertThat(publicTableCount).isGreaterThan(0);
        assertThat(rlsEnabledTableCount).isEqualTo(publicTableCount);
        assertThat(scalar(connection, """
                select relrowsecurity::text
                from pg_class c
                join pg_namespace n on n.oid = c.relnamespace
                where n.nspname = 'public'
                  and c.relname = 'flyway_schema_history'
                """)).isEqualTo("true");
        System.out.printf("RLS verification: publicTables=%d, rlsEnabled=%d, rlsDisabled=%d%n",
                publicTableCount, rlsEnabledTableCount, publicTableCount - rlsEnabledTableCount);
    }

    private static void assertNonBypassRoleCannotReadUsers(Connection connection) throws SQLException {
        String roleName = "tradepilot_rls_reader";
        execute(connection, "create role " + roleName + " nologin nosuperuser nobypassrls");
        execute(connection, "grant usage on schema public to " + roleName);
        execute(connection, "grant select on public.users to " + roleName);
        try {
            execute(connection, "set role " + roleName);
            assertThat(count(connection, "select count(*) from public.users")).isZero();
        } finally {
            execute(connection, "reset role");
            execute(connection, "revoke select on public.users from " + roleName);
            execute(connection, "revoke usage on schema public from " + roleName);
            execute(connection, "drop role " + roleName);
        }
    }

    private static void assertNullableVarcharColumn(Connection connection, String tableName, String columnName, int length) throws SQLException {
        assertVarcharColumn(connection, tableName, columnName, length, "YES");
    }

    private static void assertVarcharColumn(Connection connection, String tableName, String columnName, int length, String nullable) throws SQLException {
        assertThat(scalar(connection, """
                select character_maximum_length::text
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = '%s'
                  and column_name = '%s'
                """.formatted(tableName, columnName)))
                .isEqualTo(Integer.toString(length));
        assertThat(scalar(connection, """
                select is_nullable
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = '%s'
                  and column_name = '%s'
                """.formatted(tableName, columnName)))
                .isEqualTo(nullable);
    }

    private static void assertNegativePurchasedCreditsAreAllowed(Connection connection) {
        assertThatCode(() -> insertUserWithPurchasedCredits(connection, "postgres-flyway-negative-credits@example.com", -25))
                .doesNotThrowAnyException();
    }

    private static void assertVerifierScriptReportsOk(Connection connection) throws Exception {
        String verifier = Files.readString(Path.of("db/verify_production_schema.sql"));
        for (String rawStatement : verifier.split(";")) {
            String statement = rawStatement.trim();
            if (statement.isBlank()) {
                continue;
            }
            try (Statement jdbcStatement = connection.createStatement()) {
                boolean hasResults = jdbcStatement.execute(statement);
                if (hasResults) {
                    assertResultColumnIsOk(jdbcStatement.getResultSet(), statement);
                }
            }
        }
    }

    private static void assertResultColumnIsOk(ResultSet resultSet, String statement) throws SQLException {
        int resultColumn = -1;
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            if ("result".equalsIgnoreCase(resultSet.getMetaData().getColumnName(i))) {
                resultColumn = i;
                break;
            }
        }
        if (resultColumn < 0) {
            return;
        }

        List<String> nonOkRows = new ArrayList<>();
        while (resultSet.next()) {
            String result = resultSet.getString(resultColumn);
            if (!"OK".equals(result)) {
                nonOkRows.add(result);
            }
        }
        assertThat(nonOkRows).as("verifier statement failed: " + statement).isEmpty();
    }

    private static void assertHibernateValidatesMigratedSchema(PostgreSQLContainer<?> postgres) throws SQLException {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(TradePilotApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("test")
                .properties(
                        "JWT_SECRET=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=",
                        "app.stripe.webhook-secret=whsec_test_postgres",
                        // This test exercises a migrated PostgreSQL schema, not Stripe's API. Keep the
                        // customer-validation startup runner offline even though its fixture has Stripe IDs.
                        "app.stripe.secret-key=",
                        "app.stripe.pro-price-id=price_test_monthly",
                        "app.stripe.pro-yearly-price-id=price_test_yearly",
                        "app.frontend-url=https://tradepilot.example",
                        "tradepilot.market-data.provider=disabled"
                )
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + postgres.getJdbcUrl(),
                        "--spring.datasource.username=" + postgres.getUsername(),
                        "--spring.datasource.password=" + postgres.getPassword(),
                        "--spring.datasource.driver-class-name=org.postgresql.Driver",
                        "--spring.flyway.enabled=false",
                        "--spring.jpa.hibernate.ddl-auto=validate",
                        "--spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
                )) {
            assertThat(ignored.isActive()).isTrue();
            assertBackendDatasourceCrud(ignored.getBean(DataSource.class));
        }
    }

    private static void assertBackendDatasourceCrud(DataSource dataSource) throws SQLException {
        String email = "rls-backend-" + UUID.randomUUID() + "@example.com";
        try (Connection connection = dataSource.getConnection()) {
            long userId;
            try (PreparedStatement statement = connection.prepareStatement("""
                    insert into users(name, email, password_hash, purchased_ai_credits_balance)
                    values (?, ?, 'hash', 0)
                    returning id
                    """)) {
                statement.setString(1, "RLS backend CRUD");
                statement.setString(2, email);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).isTrue();
                    userId = resultSet.getLong(1);
                }
            }
            execute(connection, "update users set name = 'RLS backend updated' where id = " + userId);
            assertThat(scalar(connection, "select name from users where id = " + userId)).isEqualTo("RLS backend updated");
            execute(connection, "delete from users where id = " + userId);
            assertThat(count(connection, "select count(*) from users where id = " + userId)).isZero();
        }
    }

    private static long insertUser(Connection connection, String email) throws SQLException {
        return insertUserWithPurchasedCredits(connection, email, 0);
    }

    private static long insertUserWithPurchasedCredits(Connection connection, String email, int purchasedCredits) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     insert into users(name, email, password_hash, purchased_ai_credits_balance)
                     values ('Test User', '%s', 'hash', %d)
                     returning id
                     """.formatted(email, purchasedCredits))) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    private static void insertUserWithStripeIds(Connection connection, String email, String customerId, String subscriptionId) throws SQLException {
        execute(connection, """
                insert into users(name, email, password_hash, stripe_customer_id, stripe_subscription_id)
                values ('Test User', '%s', 'hash', '%s', '%s')
                """.formatted(email, customerId, subscriptionId));
    }

    private static void insertCreditPurchase(Connection connection, long userId, String checkoutSessionId, String paymentIntentId, String chargeId) throws SQLException {
        execute(connection, """
                insert into ai_credit_purchases(
                    user_id,
                    stripe_checkout_session_id,
                    stripe_payment_intent_id,
                    stripe_charge_id,
                    credits_purchased,
                    amount_cents,
                    currency,
                    status
                ) values (%d, '%s', '%s', '%s', 25, 999, 'usd', 'COMPLETED')
                """.formatted(userId, checkoutSessionId, paymentIntentId, chargeId));
    }

    private static void insertCreditPurchaseWithReversalCounts(Connection connection, long userId, String checkoutSessionId, String paymentIntentId, int restoredCredits, int disputeReversalCredits) throws SQLException {
        execute(connection, """
                insert into ai_credit_purchases(
                    user_id,
                    stripe_checkout_session_id,
                    stripe_payment_intent_id,
                    credits_purchased,
                    amount_cents,
                    currency,
                    status,
                    dispute_reversal_credits,
                    dispute_restored_credits
                ) values (%d, '%s', '%s', 25, 999, 'usd', 'COMPLETED', %d, %d)
                """.formatted(userId, checkoutSessionId, paymentIntentId, disputeReversalCredits, restoredCredits));
    }

    private static String indexDefinition(Connection connection, String indexName) throws SQLException {
        return scalar(connection, """
                select indexdef
                from pg_indexes
                where schemaname = 'public'
                  and indexname = '%s'
                """.formatted(indexName));
    }

    private static long count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    private static String scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
