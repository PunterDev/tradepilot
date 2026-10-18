package com.tradepilot.featurefunnel;

import com.tradepilot.billing.PlanType;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featurefunnel.dto.*;
import com.tradepilot.featureusage.*;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FeatureFunnelService {
    private static final String OBSERVATIONAL_CAVEAT = "This is an observational association and does not establish causation.";
    private final FeatureUsageRepository featureUsageRepository;
    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final List<FunnelDefinition> definitions = defaultDefinitions();

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FeatureFunnelDashboardResponse dashboard(String funnel, String range, LocalDate startDate, LocalDate endDate, PlanType plan, FeatureUsageCategory category, Boolean excludeAdmins, Boolean includeTestUsers, FeatureFunnelCalculationMode calculationMode, Integer conversionWindow) {
        FilterWindow window = window(range, startDate, endDate);
        List<FeatureUsageEvent> events = events(window, plan, excludeAdmins, includeTestUsers);
        Map<Long, User> usersById = usersById(events);
        List<FunnelDefinition> selected = selectedDefinitions(funnel, category, conversionWindow);
        List<CalculatedFunnel> calculated = selected.stream().map(definition -> calculate(definition, events, usersById, window)).toList();
        List<FeatureFunnelSummaryResponse> summaries = calculated.stream().map(CalculatedFunnel::summary).toList();
        List<FeatureFunnelDropoffResponse> dropoffs = summaries.stream().map(FeatureFunnelSummaryResponse::largestDropoffStep).filter(Objects::nonNull).sorted(Comparator.comparingDouble(FeatureFunnelDropoffResponse::dropoffRate).reversed()).toList();
        List<FeatureConversionAssociationResponse> conversions = calculated.stream().map(this::conversionAssociation).toList();
        List<FeatureRetentionAssociationResponse> retention = calculated.stream().map(this::retentionAssociation).toList();
        List<FeatureFunnelCohortResponse> cohorts = calculated.stream().flatMap(item -> cohorts(item).stream()).limit(12).toList();
        List<FeatureFunnelInsightResponse> insights = insights(summaries, conversions, retention);
        return new FeatureFunnelDashboardResponse(
                window.startDate(),
                window.endDate(),
                LocalDateTime.now(),
                filters(range, startDate, endDate, plan, category, excludeAdmins, includeTestUsers, calculationMode, conversionWindow),
                summaries,
                dropoffs.stream().findFirst().orElse(null),
                summaries.stream().filter(item -> item.enteredUsers() > 0).max(Comparator.comparingDouble(FeatureFunnelSummaryResponse::completionRate)).orElse(null),
                summaries.stream().filter(item -> item.medianCompletionTimeHours() != null).min(Comparator.comparingDouble(FeatureFunnelSummaryResponse::medianCompletionTimeHours)).orElse(null),
                summaries.stream().max(Comparator.comparingDouble(FeatureFunnelSummaryResponse::repeatRate14d)).orElse(null),
                conversions,
                retention,
                cohorts,
                insights,
                dataQuality(events)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FeatureFunnelSummaryResponse detail(FeatureFunnelKey funnelKey, String range, LocalDate startDate, LocalDate endDate, PlanType plan, FeatureUsageCategory category, Boolean excludeAdmins, Boolean includeTestUsers, FeatureFunnelCalculationMode calculationMode, Integer conversionWindow) {
        FilterWindow window = window(range, startDate, endDate);
        FunnelDefinition definition = definitions.stream().filter(item -> item.key() == funnelKey).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown funnel: " + funnelKey));
        if (conversionWindow != null && conversionWindow > 0) {
            definition = definition.withWindow(conversionWindow);
        }
        return calculate(definition, events(window, plan, excludeAdmins, includeTestUsers), usersById(events(window, plan, excludeAdmins, includeTestUsers)), window).summary();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<FeatureFunnelUserDrilldownResponse> users(FeatureFunnelKey funnelKey, String range, LocalDate startDate, LocalDate endDate, PlanType plan, Boolean excludeAdmins, Boolean includeTestUsers, int page, int size) {
        FilterWindow window = window(range, startDate, endDate);
        List<FeatureUsageEvent> events = events(window, plan, excludeAdmins, includeTestUsers);
        Map<Long, User> usersById = usersById(events);
        FunnelDefinition definition = definitions.stream().filter(item -> item.key() == funnelKey).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown funnel: " + funnelKey));
        CalculatedFunnel calculated = calculate(definition, events, usersById, window);
        List<Long> ids = calculated.userStates().keySet().stream().sorted().toList();
        int safeSize = Math.max(1, Math.min(size, 100));
        int from = Math.min(Math.max(0, page) * safeSize, ids.size());
        int to = Math.min(from + safeSize, ids.size());
        Map<Long, Long> tradeCounts = tradeCounts(ids.subList(from, to));
        return ids.subList(from, to).stream().map(id -> drilldown(usersById.get(id), calculated.userStates().get(id), tradeCounts.getOrDefault(id, 0L))).toList();
    }

    private List<FeatureUsageEvent> events(FilterWindow window, PlanType plan, Boolean excludeAdmins, Boolean includeTestUsers) {
        return featureUsageRepository.findFunnelEventsBetween(
                window.startDate().atStartOfDay(),
                window.endDate().plusDays(1).atStartOfDay(),
                plan,
                excludeAdmins == null || excludeAdmins,
                includeTestUsers != null && includeTestUsers,
                UserRole.ADMIN
        );
    }

    private CalculatedFunnel calculate(FunnelDefinition definition, List<FeatureUsageEvent> allEvents, Map<Long, User> usersById, FilterWindow window) {
        Map<Long, List<FeatureUsageEvent>> candidateEvents = allEvents.stream()
                .filter(event -> usersById.containsKey(event.getUserId()))
                .sorted(eventComparator())
                .collect(Collectors.groupingBy(FeatureUsageEvent::getUserId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<FeatureUsageEvent>> byUser = candidateEvents.entrySet().stream()
                .filter(entry -> eligible(definition, usersById.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        List<FeatureUsageEvent> relevant = byUser.values().stream().flatMap(List::stream).toList();
        Map<Long, UserFunnelState> states = new LinkedHashMap<>();
        for (Map.Entry<Long, List<FeatureUsageEvent>> entry : byUser.entrySet()) {
            UserFunnelState state = progress(definition, entry.getValue());
            if (state.entered()) {
                states.put(entry.getKey(), state);
            }
        }

        long eligibleUsers = relevant.stream().map(FeatureUsageEvent::getUserId).distinct().count();
        long enteredUsers = states.size();
        long completedUsers = states.values().stream().filter(UserFunnelState::completed).count();
        List<FeatureFunnelStepResponse> steps = stepResponses(definition, states.values());
        FeatureFunnelDropoffResponse largestDropoff = steps.stream()
                .filter(step -> step.stepOrder() > 1)
                .map(step -> new FeatureFunnelDropoffResponse(definition.key().name(), definition.steps().get(step.stepOrder() - 2).key(), step.stepKey(), definition.steps().get(step.stepOrder() - 2).displayName() + " → " + step.displayName(), step.dropoffCount() + step.usersReached(), step.usersReached(), step.dropoffCount(), step.dropoffRate(), step.medianTimeFromPreviousHours(), investigation(step), confidence(step.dropoffCount() + step.usersReached())))
                .max(Comparator.comparingDouble(FeatureFunnelDropoffResponse::dropoffRate))
                .orElse(null);
        List<Double> completionHours = states.values().stream().filter(UserFunnelState::completed).map(UserFunnelState::completionHours).filter(Objects::nonNull).sorted().toList();
        double repeat7 = rate(states.values().stream().filter(UserFunnelState::returned7d).count(), enteredUsers);
        double repeat14 = rate(states.values().stream().filter(UserFunnelState::returned14d).count(), enteredUsers);
        FeatureFunnelSummaryResponse summary = new FeatureFunnelSummaryResponse(
                definition.key(),
                definition.displayName(),
                definition.category(),
                definition.description(),
                Math.max(eligibleUsers, enteredUsers),
                enteredUsers,
                completedUsers,
                rate(completedUsers, enteredUsers),
                rate(completedUsers, Math.max(eligibleUsers, enteredUsers)),
                null,
                null,
                repeat7,
                repeat14,
                median(completionHours),
                average(completionHours),
                largestDropoff,
                confidence(enteredUsers),
                FeatureFunnelStatus.ACTIVE,
                steps
        );
        return new CalculatedFunnel(definition, summary, states, usersById, window);
    }

    private UserFunnelState progress(FunnelDefinition definition, List<FeatureUsageEvent> events) {
        List<FunnelStep> steps = definition.steps();
        int reachedIndex = -1;
        LocalDateTime first = null;
        LocalDateTime last = null;
        Map<Integer, LocalDateTime> reachedAt = new HashMap<>();
        LocalDateTime deadline = null;
        Set<String> seen = new HashSet<>();
        for (FeatureUsageEvent event : events) {
            LocalDateTime time = eventTime(event);
            if (deadline != null && time.isAfter(deadline)) {
                continue;
            }
            for (int next = reachedIndex + 1; next < steps.size(); next++) {
                FunnelStep step = steps.get(next);
                if (!step.required()) {
                    continue;
                }
                if (!matches(step, event)) {
                    break;
                }
                String dedupeKey = step.key() + ":" + event.getUserId() + ":" + time.toLocalDate();
                if (!seen.add(dedupeKey)) {
                    break;
                }
                reachedIndex = next;
                reachedAt.put(next, time);
                if (first == null) {
                    first = time;
                    deadline = first.plusHours(definition.conversionWindowHours());
                }
                last = time;
                break;
            }
        }
        LocalDateTime finalFirst = first;
        boolean returned7d = finalFirst != null && events.stream().anyMatch(event -> eventTime(event).isAfter(finalFirst.plusDays(1)) && !eventTime(event).isAfter(finalFirst.plusDays(7)));
        boolean returned14d = finalFirst != null && events.stream().anyMatch(event -> eventTime(event).isAfter(finalFirst.plusDays(1)) && !eventTime(event).isAfter(finalFirst.plusDays(14)));
        boolean returned30d = finalFirst != null && events.stream().anyMatch(event -> eventTime(event).isAfter(finalFirst.plusDays(1)) && !eventTime(event).isAfter(finalFirst.plusDays(30)));
        return new UserFunnelState(reachedIndex, first, last, reachedAt, reachedIndex >= lastRequiredIndex(steps), returned7d, returned14d, returned30d);
    }

    private List<FeatureFunnelStepResponse> stepResponses(FunnelDefinition definition, Collection<UserFunnelState> states) {
        long starters = states.stream().filter(UserFunnelState::entered).count();
        List<FeatureFunnelStepResponse> responses = new ArrayList<>();
        long previous = starters;
        for (int i = 0; i < definition.steps().size(); i++) {
            FunnelStep step = definition.steps().get(i);
            int index = i;
            long reached = states.stream().filter(state -> state.reachedIndex() >= index).count();
            long dropoff = i == 0 ? 0 : Math.max(0, previous - reached);
            List<Double> delays = new ArrayList<>();
            if (i > 0) {
                for (UserFunnelState state : states) {
                    LocalDateTime prev = state.reachedAt().get(i - 1);
                    LocalDateTime current = state.reachedAt().get(i);
                    if (prev != null && current != null) {
                        delays.add(Duration.between(prev, current).toMinutes() / 60.0);
                    }
                }
            }
            responses.add(new FeatureFunnelStepResponse(
                    step.key(),
                    step.displayName(),
                    step.description(),
                    step.feature(),
                    step.action(),
                    step.eventType(),
                    i + 1,
                    step.required(),
                    step.passive(),
                    reached,
                    rate(reached, starters),
                    i == 0 ? 100.0 : rate(reached, previous),
                    dropoff,
                    i == 0 ? 0.0 : rate(dropoff, previous),
                    median(delays.stream().sorted().toList()),
                    average(delays),
                    "Filtered plans",
                    "New/returning split unavailable",
                    step.passive() ? "PASSIVE" : "ACTIVE"
            ));
            previous = reached;
        }
        return responses;
    }

    private FeatureConversionAssociationResponse conversionAssociation(CalculatedFunnel calculated) {
        List<User> exposed = calculated.userStates().keySet().stream().map(calculated.usersById()::get).filter(Objects::nonNull).toList();
        Set<Long> exposedIds = exposed.stream().map(User::getId).collect(Collectors.toSet());
        List<User> comparison = calculated.usersById().values().stream().filter(user -> !exposedIds.contains(user.getId())).toList();
        long exposedConverted = exposed.stream().filter(user -> user.getPlan() == PlanType.PRO).count();
        long comparisonConverted = comparison.stream().filter(user -> user.getPlan() == PlanType.PRO).count();
        double exposedRate = rate(exposedConverted, exposed.size());
        double comparisonRate = rate(comparisonConverted, comparison.size());
        Double diff = exposed.size() + comparison.size() >= 10 ? round(exposedRate - comparisonRate) : null;
        Double lift = diff != null && comparisonRate > 0 ? round((exposedRate - comparisonRate) / comparisonRate * 100.0) : null;
        return new FeatureConversionAssociationResponse(calculated.definition().key().name(), "Entered " + calculated.definition().displayName(), exposed.size(), exposedConverted, exposedRate, comparison.size(), comparisonRate, diff, lift, exposed.size() + comparison.size(), confidence(exposed.size() + comparison.size()), OBSERVATIONAL_CAVEAT);
    }

    private FeatureRetentionAssociationResponse retentionAssociation(CalculatedFunnel calculated) {
        long cohortSize = calculated.userStates().size();
        long retained7 = calculated.userStates().values().stream().filter(UserFunnelState::returned7d).count();
        long retained14 = calculated.userStates().values().stream().filter(UserFunnelState::returned14d).count();
        long retained30 = calculated.userStates().values().stream().filter(UserFunnelState::returned30d).count();
        return new FeatureRetentionAssociationResponse(calculated.definition().key().name(), "Funnel entrants", cohortSize, retained7, rate(retained7, cohortSize), retained14, rate(retained14, cohortSize), retained30, rate(retained30, cohortSize), null, confidence(cohortSize), OBSERVATIONAL_CAVEAT);
    }

    private List<FeatureFunnelCohortResponse> cohorts(CalculatedFunnel calculated) {
        Map<String, List<UserFunnelState>> byMonth = calculated.userStates().values().stream()
                .filter(UserFunnelState::entered)
                .collect(Collectors.groupingBy(state -> state.entryAt().toLocalDate().withDayOfMonth(1).toString(), TreeMap::new, Collectors.toList()));
        return byMonth.entrySet().stream().map(entry -> {
            long entrants = entry.getValue().size();
            long completed = entry.getValue().stream().filter(UserFunnelState::completed).count();
            long r7 = entry.getValue().stream().filter(UserFunnelState::returned7d).count();
            long r14 = entry.getValue().stream().filter(UserFunnelState::returned14d).count();
            long r30 = entry.getValue().stream().filter(UserFunnelState::returned30d).count();
            return new FeatureFunnelCohortResponse(calculated.definition().key().name() + ":" + entry.getKey(), entry.getKey(), entrants, completed, rate(completed, entrants), rate(r7, entrants), rate(r14, entrants), rate(r30, entrants));
        }).toList();
    }

    private List<FeatureFunnelInsightResponse> insights(List<FeatureFunnelSummaryResponse> summaries, List<FeatureConversionAssociationResponse> conversions, List<FeatureRetentionAssociationResponse> retention) {
        List<FeatureFunnelInsightResponse> output = new ArrayList<>();
        summaries.stream().filter(summary -> summary.largestDropoffStep() != null).max(Comparator.comparingDouble(summary -> summary.largestDropoffStep().dropoffRate())).ifPresent(summary -> output.add(new FeatureFunnelInsightResponse("LARGEST_DROPOFF", "Largest funnel abandonment", summary.funnelKey().name(), summary.largestDropoffStep().transition(), summary.largestDropoffStep().dropoffUsers() + " users dropped off at " + summary.largestDropoffStep().transition() + ".", "This highlights the workflow transition most worth reviewing.", summary.largestDropoffStep().suggestedInvestigation(), "High", summary.confidence(), OBSERVATIONAL_CAVEAT)));
        summaries.stream().filter(summary -> summary.enteredUsers() > 0 && summary.completionRate() < 35).findFirst().ifPresent(summary -> output.add(new FeatureFunnelInsightResponse("HIGH_START_LOW_COMPLETION", "Users start but do not finish", summary.funnelKey().name(), null, summary.enteredUsers() + " users entered with " + summary.completionRate() + "% completion.", "Started workflows with low completion can indicate UX friction or unclear next steps.", "Review onboarding, empty states, and save-progress behavior.", "Medium", summary.confidence(), OBSERVATIONAL_CAVEAT)));
        summaries.stream().filter(summary -> summary.repeatRate14d() < 20 && summary.enteredUsers() >= 5).findFirst().ifPresent(summary -> output.add(new FeatureFunnelInsightResponse("HIGH_COMPLETION_LOW_REPEAT", "Repeat use is weak", summary.funnelKey().name(), null, summary.repeatRate14d() + "% returned within 14 days.", "Important workflows should create reasons to come back.", "Review reminders, post-completion next actions, and saved-history affordances.", "Medium", summary.confidence(), OBSERVATIONAL_CAVEAT)));
        if (output.isEmpty()) {
            output.add(new FeatureFunnelInsightResponse("INSUFFICIENT_DATA", "More funnel data needed", null, null, "Current sample sizes are small or missing action metadata.", "Small samples can make drop-off and association metrics noisy.", "Continue collecting action-level telemetry before making product conclusions.", "Low", FeatureFunnelConfidence.INSUFFICIENT, OBSERVATIONAL_CAVEAT));
        }
        return output.stream().limit(5).toList();
    }

    private List<String> dataQuality(List<FeatureUsageEvent> events) {
        List<String> warnings = new ArrayList<>();
        long missingAction = events.stream().filter(event -> event.getAction() == null).count();
        long missingUser = events.stream().filter(event -> event.getUserId() == null).count();
        if (missingAction > 0) {
            warnings.add(missingAction + " historical events are missing action metadata and may only count as broad feature entry.");
        }
        if (missingUser > 0) {
            warnings.add(missingUser + " events are missing user IDs and cannot be used in funnel sequencing.");
        }
        return warnings;
    }

    private FeatureFunnelUserDrilldownResponse drilldown(User user, UserFunnelState state, long tradeCount) {
        if (user == null || state == null) {
            return null;
        }
        return new FeatureFunnelUserDrilldownResponse(user.getId(), mask(user.getEmail()), user.getPlan(), state.stageLabel(), state.entryAt(), state.lastAt(), state.completionHours(), state.lastAt(), tradeBand(tradeCount), state.completed() ? "Completed" : "In progress");
    }

    private Map<Long, Long> tradeCounts(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : tradeRepository.countByUserIds(userIds)) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<Long, User> usersById(List<FeatureUsageEvent> events) {
        List<Long> ids = events.stream().map(FeatureUsageEvent::getUserId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private boolean eligible(FunnelDefinition definition, User user, List<FeatureUsageEvent> events) {
        if (user == null) return false;
        if (definition.planEligibility() == PlanEligibility.PRO_ONLY) return user.getPlan() == PlanType.PRO;
        if (definition.planEligibility() == PlanEligibility.FREE_ONLY) {
            return user.getPlan() == PlanType.FREE || events.stream()
                    .anyMatch(event -> matches(definition.steps().get(0), event) && PlanType.FREE.name().equals(event.getPlanAtEvent()));
        }
        return true;
    }

    private boolean matches(FunnelStep step, FeatureUsageEvent event) {
        if (event.getFeature() != step.feature()) return false;
        if (event.getAction() == null) {
            return step.order() == 1 || (step.eventType() != null && step.eventType() == event.getEventType());
        }
        return event.getAction() == step.action();
    }

    private List<FunnelDefinition> selectedDefinitions(String funnel, FeatureUsageCategory category, Integer conversionWindow) {
        return definitions.stream()
                .filter(definition -> funnel == null || funnel.isBlank() || definition.key().name().equalsIgnoreCase(funnel))
                .filter(definition -> category == null || definition.category() == category)
                .map(definition -> conversionWindow != null && conversionWindow > 0 ? definition.withWindow(conversionWindow) : definition)
                .toList();
    }

    private FilterWindow window(String range, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if ("CUSTOM".equalsIgnoreCase(range) && startDate != null && endDate != null) {
            LocalDate safeEnd = endDate.isBefore(startDate) ? startDate : endDate;
            if (safeEnd.isAfter(startDate.plusDays(365))) safeEnd = startDate.plusDays(365);
            return new FilterWindow(startDate, safeEnd);
        }
        if ("LAST_7_DAYS".equalsIgnoreCase(range)) return new FilterWindow(today.minusDays(6), today);
        if ("LAST_90_DAYS".equalsIgnoreCase(range)) return new FilterWindow(today.minusDays(89), today);
        if ("CURRENT_MONTH".equalsIgnoreCase(range)) return new FilterWindow(today.withDayOfMonth(1), today);
        if ("PREVIOUS_MONTH".equalsIgnoreCase(range)) {
            LocalDate previous = today.minusMonths(1);
            return new FilterWindow(previous.withDayOfMonth(1), previous.withDayOfMonth(previous.lengthOfMonth()));
        }
        return new FilterWindow(today.minusDays(29), today);
    }

    private Map<String, String> filters(String range, LocalDate startDate, LocalDate endDate, PlanType plan, FeatureUsageCategory category, Boolean excludeAdmins, Boolean includeTestUsers, FeatureFunnelCalculationMode calculationMode, Integer conversionWindow) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("range", range == null ? "LAST_30_DAYS" : range);
        if (startDate != null) filters.put("startDate", startDate.toString());
        if (endDate != null) filters.put("endDate", endDate.toString());
        if (plan != null) filters.put("plan", plan.name());
        if (category != null) filters.put("category", category.name());
        filters.put("excludeAdmins", String.valueOf(excludeAdmins == null || excludeAdmins));
        filters.put("includeTestUsers", String.valueOf(includeTestUsers != null && includeTestUsers));
        filters.put("calculationMode", calculationMode == null ? FeatureFunnelCalculationMode.UNIQUE_USERS.name() : calculationMode.name());
        if (conversionWindow != null) filters.put("conversionWindow", String.valueOf(conversionWindow));
        return filters;
    }

    private static Comparator<FeatureUsageEvent> eventComparator() {
        return Comparator.comparing(FeatureFunnelService::eventTime).thenComparing(event -> event.getId() == null ? 0L : event.getId());
    }

    private static LocalDateTime eventTime(FeatureUsageEvent event) {
        if (event.getOccurredAt() != null) return event.getOccurredAt();
        return event.getCreatedAt();
    }

    private static int lastRequiredIndex(List<FunnelStep> steps) {
        for (int i = steps.size() - 1; i >= 0; i--) {
            if (steps.get(i).required()) return i;
        }
        return steps.size() - 1;
    }

    private static double rate(long numerator, long denominator) {
        if (denominator <= 0) return 0.0;
        return round(numerator * 100.0 / denominator);
    }

    private static Double median(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) return round(values.get(middle));
        return round((values.get(middle - 1) + values.get(middle)) / 2.0);
    }

    private static Double average(Collection<Double> values) {
        if (values == null || values.isEmpty()) return null;
        return round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static FeatureFunnelConfidence confidence(long entrants) {
        if (entrants < 5) return FeatureFunnelConfidence.INSUFFICIENT;
        if (entrants < 20) return FeatureFunnelConfidence.LOW;
        if (entrants < 100) return FeatureFunnelConfidence.MEDIUM;
        return FeatureFunnelConfidence.HIGH;
    }

    private static String investigation(FeatureFunnelStepResponse step) {
        return "Review the " + step.displayName() + " transition, required-field clarity, empty states, and save-progress behavior.";
    }

    private static String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String maskedLocal = local.length() <= 2 ? local.charAt(0) + "*" : local.substring(0, 2) + "***";
        return maskedLocal + "@" + parts[1];
    }

    private static String tradeBand(long trades) {
        if (trades <= 0) return "0";
        if (trades < 10) return "1-9";
        if (trades < 50) return "10-49";
        if (trades < 100) return "50-99";
        return "100+";
    }

    private static List<FunnelDefinition> defaultDefinitions() {
        return List.of(
                new FunnelDefinition(FeatureFunnelKey.PLAYBOOK_ACTIVATION, "Playbook Activation Funnel", "Tracks whether users move from viewing Playbook to creating, validating, and returning to setups.", FeatureUsageCategory.PLAYBOOK, 14 * 24, PlanEligibility.PRO_OR_PREVIEW, List.of(
                        step("PLAYBOOK_VIEWED", "Playbook Viewed", FeatureKey.PLAYBOOK, FeatureUsageAction.PLAYBOOK_VIEWED, FeatureUsageEventType.PAGE_VIEW, 1, true, true),
                        step("PLAYBOOK_SETUP_STARTED", "Setup Started", FeatureKey.PLAYBOOK, FeatureUsageAction.PLAYBOOK_SETUP_STARTED, FeatureUsageEventType.USER_ACTION, 2, true, false),
                        step("PLAYBOOK_SETUP_CREATED", "Setup Created", FeatureKey.PLAYBOOK, FeatureUsageAction.PLAYBOOK_SETUP_CREATED, FeatureUsageEventType.USER_ACTION, 3, true, false),
                        step("PLAYBOOK_SETUP_COMPLETED", "Setup Definition Completed", FeatureKey.PLAYBOOK, FeatureUsageAction.PLAYBOOK_SETUP_COMPLETED, FeatureUsageEventType.USER_ACTION, 4, true, false),
                        step("PLAYBOOK_TRADE_LINKED", "First Trade Linked", FeatureKey.PLAYBOOK, FeatureUsageAction.PLAYBOOK_TRADE_LINKED, FeatureUsageEventType.USER_ACTION, 5, true, false),
                        step("PLAYBOOK_CHECKLIST_REVIEWED", "Five Trades Reviewed", FeatureKey.PLAYBOOK, FeatureUsageAction.PLAYBOOK_CHECKLIST_REVIEWED, FeatureUsageEventType.USER_ACTION, 6, true, false),
                        step("PLAYBOOK_AI_REVIEW_GENERATED", "AI Review Generated", FeatureKey.PLAYBOOK, FeatureUsageAction.PLAYBOOK_AI_REVIEW_GENERATED, FeatureUsageEventType.AI_GENERATION, 7, true, false),
                        step("PLAYBOOK_RETURNED", "Returned Within 14 Days", FeatureKey.PLAYBOOK, FeatureUsageAction.PLAYBOOK_RETURNED, FeatureUsageEventType.USER_ACTION, 8, true, false)
                )),
                new FunnelDefinition(FeatureFunnelKey.JOURNEY_ENGAGEMENT, "Journey Engagement Funnel", "Tracks Journey viewing, chapter interaction, optional AI regeneration, and return behavior.", FeatureUsageCategory.PERFORMANCE, 14 * 24, PlanEligibility.PRO_ONLY, List.of(
                        step("JOURNEY_VIEWED", "Journey Viewed", FeatureKey.PERFORMANCE_TIMELINE, FeatureUsageAction.JOURNEY_VIEWED, FeatureUsageEventType.PAGE_VIEW, 1, true, true),
                        step("JOURNEY_CHAPTER_EXPANDED", "Monthly Chapter Expanded", FeatureKey.PERFORMANCE_TIMELINE, FeatureUsageAction.JOURNEY_CHAPTER_EXPANDED, FeatureUsageEventType.USER_ACTION, 2, true, false),
                        step("JOURNEY_AI_REGENERATED", "AI Coaching Regenerated", FeatureKey.PERFORMANCE_TIMELINE, FeatureUsageAction.JOURNEY_AI_REGENERATED, FeatureUsageEventType.AI_GENERATION, 3, false, false),
                        step("JOURNEY_RETURNED", "Returned Within 14 Days", FeatureKey.PERFORMANCE_TIMELINE, FeatureUsageAction.JOURNEY_RETURNED, FeatureUsageEventType.USER_ACTION, 4, true, false)
                )),
                new FunnelDefinition(FeatureFunnelKey.LEAK_DETECTOR, "Leak Detector Funnel", "Tracks leak analysis generation, detail review, recovery plan usage, and repeat generation.", FeatureUsageCategory.PERFORMANCE, 14 * 24, PlanEligibility.PRO_ONLY, List.of(
                        step("LEAK_DETECTOR_VIEWED", "Leak Detector Viewed", FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_VIEWED, FeatureUsageEventType.PAGE_VIEW, 1, true, true),
                        step("LEAK_DETECTOR_GENERATED", "Analysis Generated", FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_GENERATED, FeatureUsageEventType.AI_GENERATION, 2, true, false),
                        step("LEAK_DETECTOR_LEAK_OPENED", "Leak Detail Opened", FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_LEAK_OPENED, FeatureUsageEventType.USER_ACTION, 3, true, false),
                        step("LEAK_DETECTOR_RECOVERY_PLAN_VIEWED", "Recovery Plan Viewed", FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_RECOVERY_PLAN_VIEWED, FeatureUsageEventType.USER_ACTION, 4, true, false),
                        step("LEAK_DETECTOR_RETURNED", "Returned Within 14 Days", FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_RETURNED, FeatureUsageEventType.USER_ACTION, 5, true, false)
                )),
                new FunnelDefinition(FeatureFunnelKey.TRADE_REVIEW, "Trade Review Funnel", "Tracks opening trade detail, generating AI review, reading feedback, and returning for another review.", FeatureUsageCategory.AI_COACHING, 7 * 24, PlanEligibility.ANY, List.of(
                        step("TRADE_REVIEW_VIEWED", "Trade Detail Viewed", FeatureKey.TRADE_DETAIL, FeatureUsageAction.TRADE_REVIEW_VIEWED, FeatureUsageEventType.PAGE_VIEW, 1, true, true),
                        step("TRADE_REVIEW_GENERATED", "AI Review Generated", FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_GENERATED, FeatureUsageEventType.AI_GENERATION, 2, true, false),
                        step("TRADE_REVIEW_ACCEPTED", "Review Read or Expanded", FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_ACCEPTED, FeatureUsageEventType.USER_ACTION, 3, true, false),
                        step("TRADE_REVIEW_RETURNED", "Returned for Another Trade Review", FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_RETURNED, FeatureUsageEventType.USER_ACTION, 4, true, false)
                )),
                new FunnelDefinition(FeatureFunnelKey.SESSION_REVIEW, "Session Review Funnel", "Tracks session page usage, generation, action item review, and return behavior.", FeatureUsageCategory.AI_COACHING, 14 * 24, PlanEligibility.PRO_ONLY, List.of(
                        step("SESSION_REVIEW_VIEWED", "Session Page Viewed", FeatureKey.SESSION_REVIEW, FeatureUsageAction.SESSION_REVIEW_VIEWED, FeatureUsageEventType.PAGE_VIEW, 1, true, true),
                        step("SESSION_REVIEW_SELECTED", "Session Selected", FeatureKey.SESSION_REVIEW, FeatureUsageAction.SESSION_REVIEW_SELECTED, FeatureUsageEventType.USER_ACTION, 2, true, false),
                        step("SESSION_REVIEW_GENERATED", "Session Review Generated", FeatureKey.SESSION_REVIEW, FeatureUsageAction.SESSION_REVIEW_GENERATED, FeatureUsageEventType.AI_GENERATION, 3, true, false),
                        step("SESSION_REVIEW_ACTION_ITEM_VIEWED", "Action Item Viewed", FeatureKey.SESSION_REVIEW, FeatureUsageAction.SESSION_REVIEW_ACTION_ITEM_VIEWED, FeatureUsageEventType.USER_ACTION, 4, true, false),
                        step("SESSION_REVIEW_RETURNED", "Returned Within 14 Days", FeatureKey.SESSION_REVIEW, FeatureUsageAction.SESSION_REVIEW_RETURNED, FeatureUsageEventType.USER_ACTION, 5, true, false)
                )),
                new FunnelDefinition(FeatureFunnelKey.FREE_TO_PRO_UPGRADE, "Free-to-Pro Upgrade Funnel", "Tracks locked feature interest through checkout and subscription activation.", FeatureUsageCategory.BILLING, 30 * 24, PlanEligibility.FREE_ONLY, List.of(
                        step("LOCKED_PREVIEW_VIEWED", "Pro Feature or Locked Preview Viewed", FeatureKey.TRADING_DNA, FeatureUsageAction.LOCKED_PREVIEW_VIEWED, FeatureUsageEventType.PAGE_VIEW, 1, true, true),
                        step("UPGRADE_CTA_CLICKED", "Upgrade CTA Clicked", FeatureKey.TRADING_DNA, FeatureUsageAction.UPGRADE_CTA_CLICKED, FeatureUsageEventType.USER_ACTION, 2, true, false),
                        step("BILLING_PAGE_VIEWED", "Billing Page Viewed", FeatureKey.PLAYBOOK, FeatureUsageAction.BILLING_PAGE_VIEWED, FeatureUsageEventType.PAGE_VIEW, 3, true, true),
                        step("CHECKOUT_STARTED", "Checkout Started", FeatureKey.PLAYBOOK, FeatureUsageAction.CHECKOUT_STARTED, FeatureUsageEventType.USER_ACTION, 4, true, false),
                        step("CHECKOUT_COMPLETED", "Checkout Completed", FeatureKey.PLAYBOOK, FeatureUsageAction.CHECKOUT_COMPLETED, FeatureUsageEventType.USER_ACTION, 5, true, false),
                        step("SUBSCRIPTION_ACTIVATED", "Subscription Activated", FeatureKey.PLAYBOOK, FeatureUsageAction.SUBSCRIPTION_ACTIVATED, FeatureUsageEventType.USER_ACTION, 6, true, false)
                )),
                new FunnelDefinition(FeatureFunnelKey.AI_CREDIT_PURCHASE, "AI Credit Purchase Funnel", "Tracks out-of-credit state through credit-pack purchase and post-purchase use.", FeatureUsageCategory.BILLING, 14 * 24, PlanEligibility.PRO_ONLY, List.of(
                        step("INSUFFICIENT_CREDIT_STATE_VIEWED", "Insufficient Credit State Viewed", FeatureKey.DAILY_COACH, FeatureUsageAction.INSUFFICIENT_CREDIT_STATE_VIEWED, FeatureUsageEventType.PAGE_VIEW, 1, true, true),
                        step("BUY_CREDITS_CTA_CLICKED", "Buy Credits CTA Clicked", FeatureKey.DAILY_COACH, FeatureUsageAction.BUY_CREDITS_CTA_CLICKED, FeatureUsageEventType.USER_ACTION, 2, true, false),
                        step("CREDIT_PACK_SELECTED", "Credit Pack Selected", FeatureKey.DAILY_COACH, FeatureUsageAction.CREDIT_PACK_SELECTED, FeatureUsageEventType.CREDIT_PURCHASE, 3, true, false),
                        step("CHECKOUT_COMPLETED", "Checkout Completed", FeatureKey.DAILY_COACH, FeatureUsageAction.CHECKOUT_COMPLETED, FeatureUsageEventType.CREDIT_PURCHASE, 4, true, false),
                        step("CREDITS_USED", "Credits Used Within 14 Days", FeatureKey.DAILY_COACH, FeatureUsageAction.CREDITS_USED, FeatureUsageEventType.AI_GENERATION, 5, true, false)
                ))
        );
    }

    private static FunnelStep step(String key, String displayName, FeatureKey feature, FeatureUsageAction action, FeatureUsageEventType eventType, int order, boolean required, boolean passive) {
        return new FunnelStep(key, displayName, displayName + " event.", feature, action, eventType, order, required, passive);
    }

    public FeatureFunnelService(FeatureUsageRepository featureUsageRepository, UserRepository userRepository, TradeRepository tradeRepository) {
        this.featureUsageRepository = featureUsageRepository;
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
    }

    private record FilterWindow(LocalDate startDate, LocalDate endDate) {}
    private enum PlanEligibility { ANY, PRO_ONLY, FREE_ONLY, PRO_OR_PREVIEW }
    private record FunnelStep(String key, String displayName, String description, FeatureKey feature, FeatureUsageAction action, FeatureUsageEventType eventType, int order, boolean required, boolean passive) {}
    private record FunnelDefinition(FeatureFunnelKey key, String displayName, String description, FeatureUsageCategory category, int conversionWindowHours, PlanEligibility planEligibility, List<FunnelStep> steps) {
        FunnelDefinition withWindow(int hours) {
            return new FunnelDefinition(key, displayName, description, category, Math.min(Math.max(hours, 1), 24 * 365), planEligibility, steps);
        }
    }
    private record UserFunnelState(int reachedIndex, LocalDateTime entryAt, LocalDateTime lastAt, Map<Integer, LocalDateTime> reachedAt, boolean completed, boolean returned7d, boolean returned14d, boolean returned30d) {
        boolean entered() { return reachedIndex >= 0; }
        Double completionHours() { return completed && entryAt != null && lastAt != null ? Duration.between(entryAt, lastAt).toMinutes() / 60.0 : null; }
        String stageLabel() { return completed ? "Completed" : "Step " + (reachedIndex + 1); }
    }
    private record CalculatedFunnel(FunnelDefinition definition, FeatureFunnelSummaryResponse summary, Map<Long, UserFunnelState> userStates, Map<Long, User> usersById, FilterWindow window) {}
}
