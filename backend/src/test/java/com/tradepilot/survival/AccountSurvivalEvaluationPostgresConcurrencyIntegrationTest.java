package com.tradepilot.survival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tradepilot.TradePilotApplication;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.survival.dto.CommitmentEvaluationResponse;
import com.tradepilot.survival.dto.CommitmentEvaluationHistoryResponse;
import com.tradepilot.survival.dto.ProtectionCommitmentRequest;
import com.tradepilot.survival.entity.NextSessionProtectionCommitment;
import com.tradepilot.survival.repository.NextSessionProtectionCommitmentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

class AccountSurvivalEvaluationPostgresConcurrencyIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static ConfigurableApplicationContext context;
    private static AccountSurvivalService service;
    private static UserRepository users;
    private static TradingAccountRepository accounts;
    private static TradeRepository trades;
    private static NextSessionProtectionCommitmentRepository commitments;
    private static TransactionTemplate transactions;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void startPostgresApplication() {
        POSTGRES.start();
        context = new SpringApplicationBuilder(TradePilotApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("test")
                .properties(
                        "JWT_SECRET=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=",
                        "app.stripe.webhook-secret=whsec_test_postgres",
                        "app.stripe.secret-key=",
                        "app.stripe.pro-price-id=price_test_monthly",
                        "app.stripe.pro-yearly-price-id=price_test_yearly",
                        "app.frontend-url=https://tradepilot.example",
                        "tradepilot.market-data.provider=disabled"
                )
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--spring.datasource.driver-class-name=org.postgresql.Driver",
                        "--spring.flyway.enabled=true",
                        "--spring.jpa.hibernate.ddl-auto=validate"
                );
        service = context.getBean(AccountSurvivalService.class);
        users = context.getBean(UserRepository.class);
        accounts = context.getBean(TradingAccountRepository.class);
        trades = context.getBean(TradeRepository.class);
        commitments = context.getBean(NextSessionProtectionCommitmentRepository.class);
        transactions = context.getBean(TransactionTemplate.class);
        jdbc = new JdbcTemplate(context.getBean(DataSource.class));
    }

    @AfterAll
    static void stopPostgresApplication() {
        if (context != null) {
            context.close();
        }
        POSTGRES.stop();
    }

    @Test
    void identicalConcurrentEvaluationsCreateOneDurableEvaluationAndOneRuleSet() throws Exception {
        Fixture fixture = committedFixture("identical");

        List<CommitmentEvaluationResponse> responses = evaluateConcurrently(fixture, false);

        assertThat(responses).allSatisfy(response -> assertThat(response.available()).isTrue());
        assertThat(responses.get(0).evaluatedSessionDate()).isEqualTo(fixture.eligibleDate());
        assertThat(responses.get(1).evaluatedSessionDate()).isEqualTo(responses.get(0).evaluatedSessionDate());
        assertThat(responses.get(1).rules()).isEqualTo(responses.get(0).rules());

        long evaluationId = singleEvaluationId(fixture.commitmentId());
        int expectedRules = responses.get(0).rules().size();
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_rule_results where evaluation_id = ?", Long.class, evaluationId))
                .isEqualTo(expectedRules);
        assertThat(jdbc.queryForObject("select count(distinct rule_type) from next_session_protection_rule_results where evaluation_id = ?", Long.class, evaluationId))
                .isEqualTo(expectedRules);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ? and is_current", Long.class, fixture.commitmentId()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations e where e.commitment_id = ? and not exists (select 1 from next_session_protection_rule_results r where r.evaluation_id = e.id)", Long.class, fixture.commitmentId()))
                .isZero();

        CommitmentEvaluationResponse later = evaluate(fixture);
        assertThat(later.rules()).isEqualTo(responses.get(0).rules());
        assertThat(singleEvaluationId(fixture.commitmentId())).isEqualTo(evaluationId);

        String fingerprint = jdbc.queryForObject("select source_fingerprint from next_session_protection_evaluations where id = ?", String.class, evaluationId);
        assertThatThrownBy(() -> jdbc.update("""
                insert into next_session_protection_evaluations(
                    user_id, account_id, commitment_id, evaluated_session_date, overall_status,
                    source_fingerprint, evaluation_version, evidence_coverage, is_current
                ) values (?, ?, ?, ?, 'COMPLETED', ?, 'account-survival-v1', ?, false)
                """, fixture.userId(), fixture.accountId(), fixture.commitmentId(), fixture.eligibleDate(), fingerprint, expectedRules))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void changedSourceCreatesOneNewAuditableVersionUnderConcurrentRequests() throws Exception {
        Fixture fixture = committedFixture("changed-source");
        evaluate(fixture);
        long originalId = singleEvaluationId(fixture.commitmentId());
        String originalFingerprint = jdbc.queryForObject("select source_fingerprint from next_session_protection_evaluations where id = ?", String.class, originalId);

        transactions.executeWithoutResult(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()), fixture.eligibleDate(),
                "13:00", "13:15", "-75.25", 1)));

        List<CommitmentEvaluationResponse> responses = evaluateConcurrently(fixture, false);
        assertThat(responses).allSatisfy(response -> assertThat(response.available()).isTrue());
        assertThat(responses.get(1).rules()).isEqualTo(responses.get(0).rules());

        List<Long> ids = jdbc.queryForList("select id from next_session_protection_evaluations where commitment_id = ? order by id", Long.class, fixture.commitmentId());
        assertThat(ids).hasSize(2);
        long currentId = jdbc.queryForObject("select id from next_session_protection_evaluations where commitment_id = ? and is_current", Long.class, fixture.commitmentId());
        assertThat(currentId).isNotEqualTo(originalId);
        assertThat(jdbc.queryForObject("select is_current from next_session_protection_evaluations where id = ?", Boolean.class, originalId)).isFalse();
        String currentFingerprint = jdbc.queryForObject("select source_fingerprint from next_session_protection_evaluations where id = ?", String.class, currentId);
        assertThat(currentFingerprint).isNotEqualTo(originalFingerprint);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_rule_results where evaluation_id = ?", Long.class, currentId))
                .isEqualTo(responses.get(0).rules().size());
        assertThat(jdbc.queryForObject("select observed_value from next_session_protection_rule_results where evaluation_id = ? and rule_type = 'MAX_ATTEMPTS'", String.class, currentId))
                .isEqualTo("3");

        evaluate(fixture);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ?", Long.class, fixture.commitmentId()))
                .isEqualTo(2);
    }

    @Test
    void postImportEvaluatorAndExplicitRequestShareTheSameDurableEvaluation() throws Exception {
        Fixture fixture = committedFixture("import-race");
        List<CommitmentEvaluationResponse> responses = evaluateConcurrently(fixture, true);

        assertThat(responses).allSatisfy(response -> assertThat(response.available()).isTrue());
        long evaluationId = singleEvaluationId(fixture.commitmentId());
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ? and is_current", Long.class, fixture.commitmentId()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_rule_results where evaluation_id = ?", Long.class, evaluationId))
                .isEqualTo(responses.get(0).rules().size());
    }

    @Test
    void evaluatesEveryLaterSessionAndVersionsOnlyTheCorrectedSession() {
        Fixture fixture = committedFixture("history");
        LocalDate secondSession = fixture.eligibleDate().plusDays(1);
        transactions.executeWithoutResult(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()), secondSession,
                "09:30", "09:45", "20.00", 1)));

        CommitmentEvaluationResponse latest = evaluate(fixture);
        assertThat(latest.evaluatedSessionDate()).isEqualTo(secondSession);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ? and is_current", Long.class, fixture.commitmentId()))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ? and evaluated_session_date = ? and is_current", Long.class, fixture.commitmentId(), fixture.eligibleDate()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from next_session_protection_commitments where id = ?", String.class, fixture.commitmentId()))
                .isEqualTo("ACCEPTED");

        transactions.executeWithoutResult(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()), fixture.eligibleDate(),
                "13:00", "13:15", "-75.25", 1)));
        evaluate(fixture);

        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ?", Long.class, fixture.commitmentId()))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ? and is_current", Long.class, fixture.commitmentId()))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ? and evaluated_session_date = ? and is_current", Long.class, fixture.commitmentId(), fixture.eligibleDate()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ? and evaluated_session_date = ? and is_current", Long.class, fixture.commitmentId(), secondSession))
                .isEqualTo(1);
    }

    @Test
    void adherenceHistoryUsesItsExplicitLatestThirtySessionWindowAndExcludesPartialSessions() {
        Fixture fixture = committedFixture("history-summary");
        LocalDate secondSession = fixture.eligibleDate().plusDays(1);
        transactions.executeWithoutResult(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()), secondSession,
                "09:30", "09:45", "20.00", 1)));
        evaluate(fixture);

        CommitmentEvaluationHistoryResponse partialOnly = transactions.execute(status -> service.evaluationHistory(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate()));
        assertThat(partialOnly.reportingWindowSessions()).isEqualTo(30);
        assertThat(partialOnly.evaluatedSessions()).isEqualTo(2);
        assertThat(partialOnly.fullyEvaluableSessions()).isZero();
        assertThat(partialOnly.adherencePercent()).isNull();
        assertThat(partialOnly.followedSessions()).isZero();

        jdbc.update("update next_session_protection_commitments set require_fresh_setup_confirmation = false where id = ?", fixture.commitmentId());
        evaluate(fixture);
        CommitmentEvaluationHistoryResponse fullyEvaluable = transactions.execute(status -> service.evaluationHistory(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate()));
        assertThat(fullyEvaluable.evaluatedSessions()).isEqualTo(2);
        assertThat(fullyEvaluable.followedSessions()).isEqualTo(1);
        assertThat(fullyEvaluable.partiallyObservedSessions()).isEqualTo(1);
        assertThat(fullyEvaluable.fullyEvaluableSessions()).isEqualTo(1);
        assertThat(fullyEvaluable.adherencePercent()).isEqualTo(100);
        assertThat(fullyEvaluable.notEvaluableSessions()).isZero();
    }

    @Test
    void doesNotRepresentHistoricalImportsAsComplianceWhenAcceptanceWasRecordedLater() {
        Fixture fixture = committedFixture("backdated");
        jdbc.update("update next_session_protection_commitments set accepted_at = ? where id = ?",
                LocalDateTime.of(2026, 4, 9, 9, 0), fixture.commitmentId());

        CommitmentEvaluationResponse response = evaluate(fixture);

        assertThat(response.available()).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from next_session_protection_evaluations where commitment_id = ?", Long.class, fixture.commitmentId()))
                .isZero();
    }

    @Test
    void concurrentDraftSaveCannotOverwriteAnAcceptedCommitment() throws Exception {
        Fixture fixture = committedFixture("save-accept");
        jdbc.update("update next_session_protection_commitments set status = 'DRAFT', accepted_at = null where id = ?", fixture.commitmentId());
        ProtectionCommitmentRequest staleDraftUpdate = new ProtectionCommitmentRequest(fixture.commitmentDate(),
                new BigDecimal("20.00"), 1, new BigDecimal("200.00"), 20, LocalTime.of(9, 30), LocalTime.of(16, 0), false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> save = executor.submit(() -> {
                ready.countDown(); await(start);
                try {
                    transactions.execute(status -> service.saveCommitment(users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), staleDraftUpdate));
                    return true;
                } catch (IllegalArgumentException expectedAfterAcceptance) {
                    return false;
                }
            });
            Future<?> accept = executor.submit(() -> {
                ready.countDown(); await(start);
                transactions.execute(status -> service.acceptCommitment(users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate()));
                return null;
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            accept.get(30, TimeUnit.SECONDS);
            save.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(jdbc.queryForObject("select status from next_session_protection_commitments where id = ?", String.class, fixture.commitmentId()))
                .isEqualTo("ACCEPTED");
        Integer acceptedMaxAttempts = jdbc.queryForObject("select max_attempts from next_session_protection_commitments where id = ?", Integer.class, fixture.commitmentId());
        assertThat(acceptedMaxAttempts).isIn(1, 3);
        assertThatThrownBy(() -> transactions.execute(status -> service.saveCommitment(users.findById(fixture.userId()).orElseThrow(),
                fixture.accountId(), new ProtectionCommitmentRequest(fixture.commitmentDate(), new BigDecimal("20.00"), 99,
                        new BigDecimal("200.00"), 20, LocalTime.of(9, 30), LocalTime.of(16, 0), false))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("select max_attempts from next_session_protection_commitments where id = ?", Integer.class, fixture.commitmentId()))
                .isEqualTo(acceptedMaxAttempts);
    }

    private static List<CommitmentEvaluationResponse> evaluateConcurrently(Fixture fixture, boolean includePostImport) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<CommitmentEvaluationResponse> explicit = executor.submit(() -> {
                ready.countDown();
                await(start);
                return evaluate(fixture);
            });
            Future<CommitmentEvaluationResponse> importTrigger = executor.submit(() -> {
                ready.countDown();
                await(start);
                if (includePostImport) {
                    User user = users.findById(fixture.userId()).orElseThrow();
                    service.evaluateEligibleAfterImport(user, fixture.accountId());
                    return evaluate(fixture);
                }
                return evaluate(fixture);
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(explicit.get(30, TimeUnit.SECONDS), importTrigger.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void await(CountDownLatch start) throws InterruptedException {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent evaluation");
        }
    }

    private static CommitmentEvaluationResponse evaluate(Fixture fixture) {
        return transactions.execute(status -> service.evaluateCommitment(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate()));
    }

    private static long singleEvaluationId(long commitmentId) {
        List<Long> ids = jdbc.queryForList("select id from next_session_protection_evaluations where commitment_id = ?", Long.class, commitmentId);
        assertThat(ids).hasSize(1);
        return ids.get(0);
    }

    private static Fixture committedFixture(String suffix) {
        return transactions.execute(status -> {
            String token = suffix + "-" + UUID.randomUUID();
            User user = users.save(User.builder()
                    .name("Concurrent survival user")
                    .email(token + "@example.com")
                    .passwordHash("hash")
                    .emailVerified(true)
                    .role(UserRole.USER)
                    .plan(PlanType.FREE)
                    .subscriptionStatus(SubscriptionStatus.NONE)
                    .build());
            TradingAccount account = accounts.save(TradingAccount.builder()
                    .user(user)
                    .name("Concurrent survival account " + token)
                    .propFirm(PropFirmType.TOPSTEP)
                    .accountType(TradingAccountType.PERSONAL)
                    .startingBalance(new BigDecimal("50000.00"))
                    .status(TradingAccountStatus.ACTIVE)
                    .build());
            LocalDate commitmentDate = LocalDate.of(2026, 4, 6);
            LocalDate eligibleDate = commitmentDate.plusDays(1);
            NextSessionProtectionCommitment commitment = new NextSessionProtectionCommitment();
            commitment.setUser(user);
            commitment.setAccount(account);
            commitment.setEffectiveSessionDate(commitmentDate);
            commitment.setMaxAttempts(3);
            commitment.setMaxNormalizedExposure(new BigDecimal("20.00"));
            commitment.setMaxSessionLoss(new BigDecimal("200.00"));
            commitment.setPostLossPauseMinutes(20);
            commitment.setAllowedWindowStart(LocalTime.of(9, 30));
            commitment.setAllowedWindowEnd(LocalTime.of(16, 0));
            commitment.setRequireFreshSetupConfirmation(true);
            commitment.setSource(ProtectionCommitmentSource.SYSTEM_RECOMMENDED);
            commitment.setStatus(ProtectionCommitmentStatus.ACCEPTED);
            commitment.setAcceptedAt(LocalDateTime.of(2026, 4, 6, 16, 5));
            commitment = commitments.save(commitment);
            trades.saveAll(List.of(
                    trade(user, account, eligibleDate, "09:30", "09:40", "-50.25", 1),
                    trade(user, account, eligibleDate, "10:15", "10:30", "25.50", 1)));
            return new Fixture(user.getId(), account.getId(), commitment.getId(), commitmentDate, eligibleDate);
        });
    }

    private static Trade trade(User user, TradingAccount account, LocalDate date, String entry, String exit, String pnl, int quantity) {
        return Trade.builder()
                .user(user)
                .account(account)
                .symbol("NQ")
                .market("FUTURES")
                .tradeDate(date)
                .entryTime(LocalDateTime.of(date, LocalTime.parse(entry)))
                .exitTime(LocalDateTime.of(date, LocalTime.parse(exit)))
                .side("LONG")
                .session(SessionType.NY_AM)
                .quantity(quantity)
                .pnlAmount(new BigDecimal(pnl))
                .outcome(new BigDecimal(pnl).signum() < 0 ? OutcomeType.LOSS : OutcomeType.WIN)
                .build();
    }

    private record Fixture(long userId, long accountId, long commitmentId, LocalDate commitmentDate, LocalDate eligibleDate) {
    }
}
