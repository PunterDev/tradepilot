package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.tradereplay.entity.TradeReplay;
import com.tradepilot.tradereplay.entity.TradeReplayInsight;
import com.tradepilot.tradereplay.entity.TradeReplayInsightType;
import com.tradepilot.tradereplay.entity.TradeReplayStatus;
import com.tradepilot.tradereplay.repository.TradeReplayInsightRepository;
import com.tradepilot.tradereplay.repository.TradeReplayRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "tradepilot.features.v3-enabled=true")
@ActiveProfiles("test")
class TradeReplaySchemaConsistencyTests {
    @Autowired UserRepository userRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired TradeReplayRepository replayRepository;
    @Autowired TradeReplayInsightRepository insightRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void replayInsightTypeConstraintMatchesJavaEnum() throws IOException {
        Set<String> expected = enumNames(TradeReplayInsightType.values());

        assertThat(allowedValues(Path.of("supabase_trade_replay_v3.sql"), "ck_trade_replay_insights_type"))
                .isEqualTo(expected);
        assertThat(allowedValues(Path.of("supabase_trade_replay_enum_constraint_sync.sql"), "ck_trade_replay_insights_type"))
                .isEqualTo(expected);
    }

    @Test
    void replayStatusConstraintMatchesJavaEnum() throws IOException {
        Set<String> expected = enumNames(TradeReplayStatus.values());

        assertThat(allowedValues(Path.of("supabase_trade_replay_v3.sql"), "ck_trade_replays_status"))
                .isEqualTo(expected);
        assertThat(allowedValues(Path.of("supabase_trade_replay_enum_constraint_sync.sql"), "ck_trade_replays_status"))
                .isEqualTo(expected);
    }

    @Test
    void persistsEveryReplayInsightTypeAndKnownSeverityValue() {
        User user = createUser("replay-schema-consistency@example.com");
        Trade trade = createTrade(user, "MESU6");
        TradeReplay replay = replayRepository.save(replay(user, trade));

        int order = 1;
        for (TradeReplayInsightType type : TradeReplayInsightType.values()) {
            TradeReplayInsight insight = new TradeReplayInsight();
            insight.setReplay(replay);
            insight.setType(type);
            insight.setSeverity(severityFor(order));
            insight.setDeterministicTitle(type.name());
            insight.setDeterministicExplanation("Schema regression insight for " + type.name());
            insight.setDisplayOrder(order++);
            insightRepository.saveAndFlush(insight);
        }

        List<TradeReplayInsight> persisted = insightRepository.findByReplayIdOrderByDisplayOrderAsc(replay.getId());
        assertThat(persisted).hasSize(TradeReplayInsightType.values().length);
        assertThat(persisted).extracting(TradeReplayInsight::getType)
                .containsExactly(TradeReplayInsightType.values());
        assertThat(persisted).extracting(TradeReplayInsight::getSeverity)
                .contains("INFO", "POSITIVE", "WATCH", "IMPORTANT");
    }

    private static Set<String> allowedValues(Path path, String constraintName) throws IOException {
        String sql = Files.readString(path);
        Pattern constraint = Pattern.compile("(?is)" + Pattern.quote(constraintName) + ".*?\\bin\\s*\\((.*?)\\)\\s*\\)");
        Matcher matcher = constraint.matcher(sql);
        assertThat(matcher.find()).as("constraint %s in %s", constraintName, path).isTrue();

        Pattern quoted = Pattern.compile("'([^']+)'");
        Matcher values = quoted.matcher(matcher.group(1));
        Set<String> allowed = new TreeSet<>();
        while (values.find()) {
            allowed.add(values.group(1));
        }
        return allowed;
    }

    private static <E extends Enum<E>> Set<String> enumNames(E[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toCollection(TreeSet::new));
    }

    private String severityFor(int order) {
        return switch (order % 4) {
            case 1 -> "INFO";
            case 2 -> "POSITIVE";
            case 3 -> "WATCH";
            default -> "IMPORTANT";
        };
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("Replay Schema User")
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    private Trade createTrade(User user, String contractSymbol) {
        Trade trade = Trade.builder()
                .user(user)
                .symbol("MES")
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 6, 8))
                .entryTime(LocalDateTime.of(2026, 6, 8, 5, 25))
                .exitTime(LocalDateTime.of(2026, 6, 8, 5, 28))
                .side("SHORT")
                .session(SessionType.NY_AM)
                .entryPrice(new BigDecimal("29190.00"))
                .exitPrice(new BigDecimal("29178.00"))
                .quantity(3)
                .pnlAmount(new BigDecimal("72.00"))
                .outcome(OutcomeType.WIN)
                .build();
        trade.setContractSymbol(contractSymbol);
        trade.setOriginalBrokerSymbol(contractSymbol);
        return tradeRepository.save(trade);
    }

    private TradeReplay replay(User user, Trade trade) {
        TradeReplay replay = new TradeReplay();
        replay.setUser(user);
        replay.setTrade(trade);
        replay.setStatus(TradeReplayStatus.READY);
        replay.setOriginalBrokerSymbol(trade.getOriginalBrokerSymbol());
        replay.setResolvedProviderSymbol(trade.getContractSymbol());
        replay.setProvider("databento");
        replay.setCandleInterval("ohlcv-1m");
        return replay;
    }
}
