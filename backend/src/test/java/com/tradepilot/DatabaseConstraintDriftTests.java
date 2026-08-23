package com.tradepilot;

import com.tradepilot.billing.UsageType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConstraintDriftTests {
    @Test
    void featureUsageConstraintContainsEveryFeatureKey() throws IOException {
        Set<String> allowed = valuesInConstraint(Path.of("supabase_playbook.sql"), "feature");
        Set<String> enumValues = names(FeatureKey.values());

        assertThat(allowed).containsAll(enumValues);
        assertThat(allowed).contains("TRADING_MEMORY");
    }

    @Test
    void featureUsageActionConstraintContainsEveryAction() throws IOException {
        Set<String> canonicalAllowed = valuesInConstraint(Path.of("supabase_feature_funnels.sql"), "action");
        Set<String> compatibilityAllowed = valuesInConstraint(Path.of("supabase_feature_usage_events_add_trading_memory_actions.sql"), "action");
        Set<String> enumValues = names(FeatureUsageAction.values());

        assertThat(canonicalAllowed).containsAll(enumValues);
        assertThat(compatibilityAllowed).containsAll(enumValues);
        assertThat(compatibilityAllowed).contains(
                "TRADING_MEMORY_VIEWED",
                "TRADING_MEMORY_REGENERATED",
                "SIMILAR_TRADE_OPENED",
                "TRADING_MEMORY_DETERMINISTIC_INSIGHT_VIEWED",
                "TRADING_MEMORY_AI_INSIGHT_VIEWED",
                "TRADING_MEMORY_PATTERN_DETECTED"
        );
    }

    @Test
    void aiUsageConstraintContainsEveryUsageType() throws IOException {
        Set<String> allowed = valuesInConstraint(Path.of("supabase_playbook.sql"), "usage_type");
        Set<String> enumValues = names(UsageType.values());

        assertThat(allowed).containsAll(enumValues);
    }

    @Test
    void tradeSessionConstraintContainsEverySessionType() throws IOException {
        Set<String> allowed = valuesInConstraint(Path.of("supabase_trade_session_unknown.sql"), "session");
        Set<String> enumValues = names(SessionType.values());

        assertThat(allowed).containsAll(enumValues);
    }

    private Set<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toSet());
    }

    private Set<String> valuesInConstraint(Path sqlPath, String columnName) throws IOException {
        String sql = Files.readString(sqlPath);
        Pattern pattern = Pattern.compile("(?:"
                + columnName + "\\s+IS\\s+NULL\\s+OR\\s+)?"
                + columnName + "\\s+IN\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(sql);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        assertThat(lastMatch)
                .as("No CHECK IN list found for %s in %s", columnName, sqlPath)
                .isNotNull();
        return Pattern.compile("'([^']+)'")
                .matcher(lastMatch)
                .results()
                .map(result -> result.group(1))
                .collect(Collectors.toSet());
    }
}
