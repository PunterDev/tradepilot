package com.tradepilot;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
                .load();

        MigrateResult firstMigration = flyway.migrate();
        assertThat(firstMigration.migrationsExecuted).isEqualTo(1);

        try (Connection connection = connect(postgres)) {
            assertThat(scalar(connection, "select version()")).contains("PostgreSQL 16");
            assertThat(scalar(connection, "select version from flyway_schema_history where version = '001' and success = true"))
                    .isEqualTo("001");

            assertRequiredTables(connection);
            assertStripeIndexesAndConstraints(connection);
            assertNegativePurchasedCreditsAreAllowed(connection);
            assertVerifierScriptReportsOk(connection);
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
                "stripe_checkout_attempts"
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

        assertThatThrownBy(() -> insertCreditPurchaseWithReversalCounts(connection, userId, "cs_bad_reversal", "pi_bad_reversal", 10, 5))
                .isInstanceOf(SQLException.class);

        assertThat(count(connection, """
                select count(*)
                from pg_constraint
                where connamespace = 'public'::regnamespace
                  and conname = 'users_purchased_ai_credits_balance_nonnegative'
                """)).isZero();
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

    private static void assertHibernateValidatesMigratedSchema(PostgreSQLContainer<?> postgres) {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(TradePilotApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("test")
                .properties(
                        "server.port=0",
                        "spring.datasource.url=" + postgres.getJdbcUrl(),
                        "spring.datasource.username=" + postgres.getUsername(),
                        "spring.datasource.password=" + postgres.getPassword(),
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.flyway.enabled=false",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
                        "JWT_SECRET=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=",
                        "app.stripe.webhook-secret=whsec_test_postgres",
                        "app.stripe.secret-key=sk_test_postgres",
                        "app.stripe.pro-price-id=price_test_monthly",
                        "app.stripe.pro-yearly-price-id=price_test_yearly",
                        "app.frontend-url=https://tradepilot.example",
                        "tradepilot.market-data.provider=disabled"
                )
                .run()) {
            assertThat(ignored.isActive()).isTrue();
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
