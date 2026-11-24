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
import com.tradepilot.survival.dto.ProtectionCommitmentResponse;
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
        int historicalDuplicate = jdbc.update("""
                insert into next_session_protection_evaluations(
                    user_id, account_id, commitment_id, evaluated_session_date, overall_status,
                    source_fingerprint, evaluation_version, evidence_coverage, is_current
                ) values (?, ?, ?, ?, 'COMPLETED', ?, 'account-survival-v1', ?, false)
                """, fixture.userId(), fixture.accountId(), fixture.commitmentId(), fixture.eligibleDate(), fingerprint, expectedRules);
        assertThat(historicalDuplicate).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("""
                insert into next_session_protection_evaluations(
                    user_id, account_id, commitment_id, evaluated_session_date, overall_status,
                    source_fingerprint, evaluation_version, evidence_coverage, is_current
                ) values (?, ?, ?, ?, 'COMPLETED', 'different-current-fingerprint', 'account-survival-v1', ?, true)
                """, fixture.userId(), fixture.accountId(), fixture.commitmentId(), fixture.eligibleDate(), expectedRules))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("delete from next_session_protection_evaluations where commitment_id = ? and is_current = false",
                fixture.commitmentId());
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
    void evidenceReversionAppendsThirdEventAndRepeatedEquivalentRequestsRemainIdempotent() throws Exception {
        Fixture fixture = committedFixture("evidence-reversion");
        CommitmentEvaluationResponse original = evaluate(fixture);
        long firstId = singleEvaluationId(fixture.commitmentId());
        LocalDateTime firstCreatedAt = jdbc.queryForObject(
                "select created_at from next_session_protection_evaluations where id = ?",
                LocalDateTime.class, firstId);

        Long addedTradeId = transactions.execute(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()),
                fixture.eligibleDate(), "13:00", "13:15", "-75.25", 1)).getId());
        evaluate(fixture);
        long secondId = jdbc.queryForObject(
                "select id from next_session_protection_evaluations where commitment_id = ? and is_current",
                Long.class, fixture.commitmentId());

        transactions.executeWithoutResult(status -> trades.deleteById(addedTradeId));
        List<CommitmentEvaluationResponse> reverted = evaluateConcurrently(fixture, false);
        assertThat(reverted).allSatisfy(response -> assertThat(response.rules()).isEqualTo(original.rules()));

        List<Long> eventIds = jdbc.queryForList(
                "select id from next_session_protection_evaluations where commitment_id = ? order by id",
                Long.class, fixture.commitmentId());
        assertThat(eventIds).hasSize(3);
        long thirdId = eventIds.get(2);
        assertThat(firstId).isLessThan(secondId);
        assertThat(secondId).isLessThan(thirdId);
        assertThat(jdbc.queryForObject(
                "select source_fingerprint from next_session_protection_evaluations where id = ?",
                String.class, thirdId)).isEqualTo(jdbc.queryForObject(
                "select source_fingerprint from next_session_protection_evaluations where id = ?",
                String.class, firstId));
        LocalDateTime thirdCreatedAt = jdbc.queryForObject(
                "select created_at from next_session_protection_evaluations where id = ?",
                LocalDateTime.class, thirdId);
        assertThat(firstCreatedAt).isNotNull();
        assertThat(thirdCreatedAt).isAfter(firstCreatedAt);
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_evaluations where commitment_id = ? and is_current",
                Long.class, fixture.commitmentId())).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_rule_results where evaluation_id = ?",
                Long.class, firstId)).isEqualTo(original.rules().size());
        assertThat(jdbc.queryForObject("""
                select count(*) from next_session_protection_evaluations e
                where e.commitment_id = ?
                  and not exists (select 1 from next_session_protection_rule_results r where r.evaluation_id = e.id)
                """, Long.class, fixture.commitmentId())).isZero();

        evaluate(fixture);
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_evaluations where commitment_id = ?",
                Long.class, fixture.commitmentId())).isEqualTo(3);
    }

    @Test
    void calculationVersionParticipatesInFingerprintAndPreservesPriorResults() {
        Fixture fixture = committedFixture("calculation-version");
        CommitmentEvaluationResponse versionOne = evaluate(fixture);
        long versionOneId = singleEvaluationId(fixture.commitmentId());

        CommitmentEvaluationResponse versionTwo = transactions.execute(status ->
                service.evaluateCommitmentForCalculationVersion(
                        users.findById(fixture.userId()).orElseThrow(), fixture.accountId(),
                        fixture.commitmentDate(), "account-survival-v2-test"));
        long versionTwoId = jdbc.queryForObject(
                "select id from next_session_protection_evaluations where commitment_id = ? and is_current",
                Long.class, fixture.commitmentId());

        assertThat(versionTwoId).isNotEqualTo(versionOneId);
        assertThat(versionTwo.evaluationVersion()).isEqualTo("account-survival-v2-test");
        assertThat(versionTwo.rules()).isEqualTo(versionOne.rules());
        assertThat(jdbc.queryForObject(
                "select evaluation_version from next_session_protection_evaluations where id = ?",
                String.class, versionOneId)).isEqualTo(AccountSurvivalService.CALCULATION_VERSION);
        assertThat(jdbc.queryForObject(
                "select source_fingerprint from next_session_protection_evaluations where id = ?",
                String.class, versionTwoId)).isNotEqualTo(jdbc.queryForObject(
                "select source_fingerprint from next_session_protection_evaluations where id = ?",
                String.class, versionOneId));

        transactions.execute(status -> service.evaluateCommitmentForCalculationVersion(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(),
                fixture.commitmentDate(), "account-survival-v2-test"));
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_evaluations where commitment_id = ?",
                Long.class, fixture.commitmentId())).isEqualTo(2);
    }

    @Test
    void correctingMissingPnlToGenuineZeroAppendsEvaluation() {
        Fixture fixture = committedFixture("missing-to-zero");
        Long tradeId = jdbc.queryForObject(
                "select min(id) from trades where account_id = ? and trade_date = ?",
                Long.class, fixture.accountId(), fixture.eligibleDate());
        jdbc.update("update trades set pnl_amount = null where id = ?", tradeId);
        evaluate(fixture);
        long missingPnlEvaluation = singleEvaluationId(fixture.commitmentId());
        assertThat(jdbc.queryForObject("""
                select result from next_session_protection_rule_results
                where evaluation_id = ? and rule_type = 'MAX_SESSION_LOSS'
                """, String.class, missingPnlEvaluation)).isEqualTo("NOT_EVALUABLE");

        jdbc.update("update trades set pnl_amount = 0.00 where id = ?", tradeId);
        evaluate(fixture);
        List<Long> ids = jdbc.queryForList(
                "select id from next_session_protection_evaluations where commitment_id = ? order by id",
                Long.class, fixture.commitmentId());
        assertThat(ids).hasSize(2);
        assertThat(jdbc.queryForObject(
                "select source_fingerprint from next_session_protection_evaluations where id = ?",
                String.class, ids.get(1))).isNotEqualTo(jdbc.queryForObject(
                "select source_fingerprint from next_session_protection_evaluations where id = ?",
                String.class, ids.get(0)));
        assertThat(jdbc.queryForObject("""
                select result from next_session_protection_rule_results
                where evaluation_id = ? and rule_type = 'MAX_SESSION_LOSS'
                """, String.class, ids.get(0))).isEqualTo("NOT_EVALUABLE");
        assertThat(jdbc.queryForObject("""
                select result from next_session_protection_rule_results
                where evaluation_id = ? and rule_type = 'MAX_SESSION_LOSS'
                """, String.class, ids.get(1))).isEqualTo("FOLLOWED");
    }

    @Test
    void historyUsesDatabaseEventIdsWhenTimestampsAreEqualOrMisordered() {
        Fixture fixture = committedFixture("history-id-order");
        evaluate(fixture);
        Long addedTradeId = transactions.execute(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()),
                fixture.eligibleDate(), "13:00", "13:15", "-75.25", 1)).getId());
        evaluate(fixture);
        transactions.executeWithoutResult(status -> trades.deleteById(addedTradeId));
        evaluate(fixture);
        List<Long> ascending = jdbc.queryForList(
                "select id from next_session_protection_evaluations where commitment_id = ? order by id",
                Long.class, fixture.commitmentId());
        jdbc.update("update next_session_protection_evaluations set created_at = timestamp '2099-01-01 00:00:00' where id = ?",
                ascending.get(0));
        jdbc.update("update next_session_protection_evaluations set created_at = timestamp '2000-01-01 00:00:00' where id in (?, ?)",
                ascending.get(1), ascending.get(2));

        CommitmentEvaluationHistoryResponse history = transactions.execute(status -> service.evaluationHistory(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate(), 10));
        assertThat(history.evaluations()).extracting(CommitmentEvaluationHistoryResponse.Entry::evaluationId)
                .containsExactly(ascending.get(2), ascending.get(1), ascending.get(0));
    }

    @Test
    void historyPreservesEveryVersionWithRulesAndKeepsLatestSessionCompatible() {
        Fixture fixture = committedFixture("durable-history");
        LocalDate secondSession = fixture.eligibleDate().plusDays(1);
        transactions.executeWithoutResult(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()), secondSession,
                "09:30", "09:45", "20.00", 1)));

        CommitmentEvaluationResponse initialLatest = evaluate(fixture);
        assertThat(initialLatest.evaluatedSessionDate()).isEqualTo(secondSession);
        CommitmentEvaluationHistoryResponse bounded = transactions.execute(status -> service.evaluationHistory(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate(), 1));
        assertThat(bounded.evaluations()).hasSize(1);
        assertThat(bounded.hasMore()).isTrue();
        assertThat(bounded.evaluations().get(0).evaluatedSessionDate()).isEqualTo(secondSession);
        assertThat(bounded.evaluations().get(0).evaluatedAt()).isNotNull();
        assertThat(bounded.evaluations().get(0).rules()).isNotEmpty();

        CommitmentEvaluationHistoryResponse beforeCorrection = transactions.execute(status -> service.evaluationHistory(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate(), 10));
        CommitmentEvaluationHistoryResponse.Entry originalFirstSession = beforeCorrection.evaluations().stream()
                .filter(entry -> entry.evaluatedSessionDate().equals(fixture.eligibleDate()))
                .findFirst().orElseThrow();

        transactions.executeWithoutResult(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()), fixture.eligibleDate(),
                "13:00", "13:15", "-75.25", 1)));
        evaluate(fixture);

        CommitmentEvaluationHistoryResponse afterCorrection = transactions.execute(status -> service.evaluationHistory(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate(), 10));
        assertThat(afterCorrection.accountId()).isEqualTo(fixture.accountId());
        assertThat(afterCorrection.commitmentId()).isEqualTo(fixture.commitmentId());
        assertThat(afterCorrection.evaluations()).hasSize(3);
        assertThat(afterCorrection.evaluations().stream().filter(CommitmentEvaluationHistoryResponse.Entry::current)).hasSize(2);
        assertThat(afterCorrection.evaluations().stream()
                .filter(entry -> entry.evaluatedSessionDate().equals(fixture.eligibleDate()))).hasSize(2);
        CommitmentEvaluationHistoryResponse.Entry preserved = afterCorrection.evaluations().stream()
                .filter(entry -> entry.evaluationId().equals(originalFirstSession.evaluationId()))
                .findFirst().orElseThrow();
        assertThat(preserved.current()).isFalse();
        assertThat(preserved.rules()).isEqualTo(originalFirstSession.rules());

        CommitmentEvaluationResponse current = transactions.execute(status -> service.currentEvaluation(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate()));
        assertThat(current.evaluatedSessionDate()).isEqualTo(secondSession);
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_evaluations where commitment_id = ? and is_current",
                Long.class, fixture.commitmentId())).isEqualTo(2);
    }

    @Test
    void failedOuterEvaluationTransactionPreservesPreviousCurrentHistory() {
        Fixture fixture = committedFixture("history-rollback");
        CommitmentEvaluationResponse original = evaluate(fixture);
        long originalId = singleEvaluationId(fixture.commitmentId());
        transactions.executeWithoutResult(status -> trades.save(trade(
                users.getReferenceById(fixture.userId()), accounts.getReferenceById(fixture.accountId()),
                fixture.eligibleDate(), "13:00", "13:15", "-75.25", 1)));

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            service.evaluateCommitment(users.findById(fixture.userId()).orElseThrow(),
                    fixture.accountId(), fixture.commitmentDate());
            throw new IllegalStateException("force rollback after evaluation persistence");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_evaluations where commitment_id = ?",
                Long.class, fixture.commitmentId())).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select id from next_session_protection_evaluations where commitment_id = ? and is_current",
                Long.class, fixture.commitmentId())).isEqualTo(originalId);
        assertThat(jdbc.queryForObject("""
                select count(*) from next_session_protection_evaluations e
                where e.commitment_id = ?
                  and not exists (select 1 from next_session_protection_rule_results r where r.evaluation_id = e.id)
                """, Long.class, fixture.commitmentId())).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_rule_results where evaluation_id = ?",
                Long.class, originalId)).isEqualTo(original.rules().size());
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
    void concurrentAndRepeatedAcceptancePreserveOneTimestampAndOneRecord() throws Exception {
        Fixture fixture = committedFixture("accept-idempotency");
        jdbc.update("update next_session_protection_commitments set status = 'DRAFT', accepted_at = null where id = ?",
                fixture.commitmentId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<ProtectionCommitmentResponse> responses;
        try {
            Future<ProtectionCommitmentResponse> first = executor.submit(() -> acceptAfterStart(fixture, ready, start));
            Future<ProtectionCommitmentResponse> second = executor.submit(() -> acceptAfterStart(fixture, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            responses = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.id()).isEqualTo(fixture.commitmentId());
            assertThat(response.status()).isEqualTo(ProtectionCommitmentStatus.ACCEPTED);
            assertThat(response.acceptedAt()).isNotNull();
        });
        assertThat(responses.get(1).acceptedAt()).isEqualTo(responses.get(0).acceptedAt());
        ProtectionCommitmentResponse repeated = transactions.execute(status -> service.acceptCommitment(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate()));
        assertThat(repeated.acceptedAt()).isEqualTo(responses.get(0).acceptedAt());
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_commitments where account_id = ? and effective_session_date = ?",
                Long.class, fixture.accountId(), fixture.commitmentDate())).isEqualTo(1);
    }

    @Test
    void repeatedAcceptanceDoesNotResetEvaluationState() {
        Fixture fixture = committedFixture("accept-after-evaluation");
        evaluate(fixture);
        String evaluatedStatus = jdbc.queryForObject(
                "select status from next_session_protection_commitments where id = ?", String.class, fixture.commitmentId());
        LocalDateTime acceptedAtBefore = jdbc.queryForObject(
                "select accepted_at from next_session_protection_commitments where id = ?", LocalDateTime.class,
                fixture.commitmentId());
        Long evaluationCount = jdbc.queryForObject(
                "select count(*) from next_session_protection_evaluations where commitment_id = ?", Long.class,
                fixture.commitmentId());

        ProtectionCommitmentResponse repeated = transactions.execute(status -> service.acceptCommitment(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate()));

        assertThat(repeated.status().name()).isEqualTo(evaluatedStatus);
        assertThat(repeated.acceptedAt()).isNotNull();
        assertThat(jdbc.queryForObject(
                "select accepted_at from next_session_protection_commitments where id = ?", LocalDateTime.class,
                fixture.commitmentId())).isEqualTo(acceptedAtBefore);
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_evaluations where commitment_id = ?", Long.class,
                fixture.commitmentId())).isEqualTo(evaluationCount);
    }

    @Test
    void concurrentSaveAndAcceptanceCannotMutateAcceptedTerms() throws Exception {
        Fixture fixture = committedFixture("save-accept");
        jdbc.update("update next_session_protection_commitments set status = 'DRAFT', accepted_at = null where id = ?",
                fixture.commitmentId());
        ProtectionCommitmentRequest update = request(fixture.commitmentDate(), 1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> save = executor.submit(() -> {
                ready.countDown();
                await(start);
                try {
                    transactions.execute(status -> service.saveCommitment(
                            users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), update));
                    return true;
                } catch (ProtectionCommitmentLifecycleException expectedAfterAcceptance) {
                    return false;
                }
            });
            Future<ProtectionCommitmentResponse> accept = executor.submit(() -> acceptAfterStart(fixture, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            accept.get(30, TimeUnit.SECONDS);
            save.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        Integer acceptedMaxAttempts = jdbc.queryForObject(
                "select max_attempts from next_session_protection_commitments where id = ?", Integer.class,
                fixture.commitmentId());
        assertThat(acceptedMaxAttempts).isIn(1, 3);
        assertThat(jdbc.queryForObject(
                "select status from next_session_protection_commitments where id = ?", String.class,
                fixture.commitmentId())).isEqualTo("ACCEPTED");
        assertThatThrownBy(() -> transactions.execute(status -> service.saveCommitment(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), request(fixture.commitmentDate(), 99))))
                .isInstanceOf(ProtectionCommitmentLifecycleException.class);
        assertThat(jdbc.queryForObject(
                "select max_attempts from next_session_protection_commitments where id = ?", Integer.class,
                fixture.commitmentId())).isEqualTo(acceptedMaxAttempts);
    }

    @Test
    void concurrentDraftSavesSerializeWithoutDuplicateOrMixedTerms() throws Exception {
        Fixture fixture = committedFixture("draft-saves");
        jdbc.update("delete from next_session_protection_commitments where id = ?", fixture.commitmentId());

        runConcurrentSaves(fixture, request(fixture.commitmentDate(), 1), request(fixture.commitmentDate(), 2));
        assertSingleCompleteDraft(fixture, 1, 2);

        runConcurrentSaves(fixture, request(fixture.commitmentDate(), 4), request(fixture.commitmentDate(), 5));
        assertSingleCompleteDraft(fixture, 4, 5);
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

    private static ProtectionCommitmentResponse acceptAfterStart(
            Fixture fixture, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        await(start);
        return transactions.execute(status -> service.acceptCommitment(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), fixture.commitmentDate()));
    }

    private static void runConcurrentSaves(
            Fixture fixture, ProtectionCommitmentRequest firstRequest, ProtectionCommitmentRequest secondRequest)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> {
                saveAfterStart(fixture, firstRequest, ready, start);
                return null;
            });
            Future<?> second = executor.submit(() -> {
                saveAfterStart(fixture, secondRequest, ready, start);
                return null;
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void saveAfterStart(
            Fixture fixture, ProtectionCommitmentRequest request, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        await(start);
        transactions.executeWithoutResult(status -> service.saveCommitment(
                users.findById(fixture.userId()).orElseThrow(), fixture.accountId(), request));
    }

    private static void assertSingleCompleteDraft(Fixture fixture, int firstAttempts, int secondAttempts) {
        assertThat(jdbc.queryForObject(
                "select count(*) from next_session_protection_commitments where account_id = ? and effective_session_date = ?",
                Long.class, fixture.accountId(), fixture.commitmentDate())).isEqualTo(1);
        Integer attempts = jdbc.queryForObject(
                "select max_attempts from next_session_protection_commitments where account_id = ? and effective_session_date = ?",
                Integer.class, fixture.accountId(), fixture.commitmentDate());
        BigDecimal exposure = jdbc.queryForObject(
                "select max_normalized_exposure from next_session_protection_commitments where account_id = ? and effective_session_date = ?",
                BigDecimal.class, fixture.accountId(), fixture.commitmentDate());
        assertThat(attempts).isIn(firstAttempts, secondAttempts);
        assertThat(exposure).isEqualByComparingTo(BigDecimal.valueOf(attempts * 10L));
        assertThat(jdbc.queryForObject(
                "select status from next_session_protection_commitments where account_id = ? and effective_session_date = ?",
                String.class, fixture.accountId(), fixture.commitmentDate())).isEqualTo("DRAFT");
    }

    private static ProtectionCommitmentRequest request(LocalDate date, int attempts) {
        return new ProtectionCommitmentRequest(date, BigDecimal.valueOf(attempts * 10L), attempts,
                new BigDecimal("200.00"), 20, LocalTime.of(9, 30), LocalTime.of(16, 0), false);
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
