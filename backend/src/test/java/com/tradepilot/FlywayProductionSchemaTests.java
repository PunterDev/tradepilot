package com.tradepilot;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayProductionSchemaTests {
    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V001__fresh_production_schema.sql");
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
    void productionVerifierChecksTheSameStripeSafetyObjects() throws IOException {
        String sql = Files.readString(VERIFIER);

        assertThat(sql).contains("ux_users_stripe_customer_id_not_null");
        assertThat(sql).contains("ux_users_stripe_subscription_id_not_null");
        assertThat(sql).contains("uk_ai_credit_purchases_checkout_session");
        assertThat(sql).contains("ux_ai_credit_purchases_payment_intent_not_null");
        assertThat(sql).contains("chk_ai_credit_purchases_dispute_restored_lte_reversal");
        assertThat(sql).contains("obsolete_constraint_absent");
    }
}
