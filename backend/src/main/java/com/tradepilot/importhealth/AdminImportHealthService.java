package com.tradepilot.importhealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEvent;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.importhealth.dto.AdminImportHealthSummaryResponse;
import com.tradepilot.importhealth.dto.AdminRecentImportOperationResponse;
import com.tradepilot.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminImportHealthService {
    private static final int RECENT_LIMIT = 100;
    private final FeatureUsageRepository events;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    public AdminImportHealthService(FeatureUsageRepository events, UserRepository users, ObjectMapper objectMapper) {
        this.events = events;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminImportHealthSummaryResponse summary(Range range) {
        Window window = window(range);
        List<Operation> operations = operations(window);
        List<Operation> saves = operations.stream().filter(Operation::isSaveOperation).toList();
        long completed = saves.stream().filter(Operation::completed).count();
        long failed = saves.stream().filter(Operation::failed).count();
        long previewFailures = operations.stream().filter(operation -> operation.previewFailed()).count();
        long saved = operations.stream().filter(Operation::completed).mapToLong(operation -> operation.value("savedRows")).sum();
        long duplicates = operations.stream().filter(Operation::completed).mapToLong(operation -> operation.value("duplicateRows")).sum();
        long firstImports = operations.stream().filter(operation -> operation.event().getAction() == FeatureUsageAction.FIRST_IMPORT_COMPLETED).count();
        Map<String, Long> parser = new TreeMap<>();
        operations.stream().filter(operation -> operation.completed() || operation.failed()).forEach(operation -> parser.merge(operation.parserSource(), 1L, Long::sum));
        List<AdminImportHealthSummaryResponse.FailureRow> failures = operations.stream().filter(Operation::failed)
                .collect(java.util.stream.Collectors.groupingBy(operation -> operation.stage() + "|" + operation.failureCategory(), TreeMap::new, java.util.stream.Collectors.counting()))
                .entrySet().stream().map(entry -> { String[] parts = entry.getKey().split("\\|", 2); return new AdminImportHealthSummaryResponse.FailureRow(parts[0], parts[1], entry.getValue()); }).toList();
        return new AdminImportHealthSummaryResponse(window.start(), window.end(), saves.size(), completed, failed,
                saves.isEmpty() ? null : roundPercent(completed, saves.size()), previewFailures, saved, duplicates, firstImports,
                parser.entrySet().stream().map(entry -> new AdminImportHealthSummaryResponse.CountRow(entry.getKey(), entry.getValue())).toList(), failures, LocalDateTime.now());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AdminRecentImportOperationResponse> recent(Range range) {
        Map<Long, User> byId = users.findAllById(operations(window(range)).stream().map(operation -> operation.event().getUserId()).distinct().toList()).stream().collect(java.util.stream.Collectors.toMap(User::getId, user -> user));
        return operations(window(range)).stream().filter(operation -> operation.isSaveOperation() || operation.previewFailed()).limit(RECENT_LIMIT).map(operation -> new AdminRecentImportOperationResponse(
                occurredAt(operation.event()), mask(byId.get(operation.event().getUserId())), operation.event().getAccountId(), operation.stage(), operation.status(), operation.parserSource(),
                operation.number("parsedTrades"), operation.number("savedRows"), operation.number("duplicateRows"), operation.failed() ? operation.failureCategory() : null,
                operation.event().getAction() == FeatureUsageAction.FIRST_IMPORT_COMPLETED)).toList();
    }

    private List<Operation> operations(Window window) {
        return events.findByCreatedAtGreaterThanEqual(window.start().atStartOfDay()).stream()
                .filter(event -> occurredAt(event).isBefore(window.end().plusDays(1).atStartOfDay()))
                .filter(event -> event.getFeature() == FeatureKey.ACTIVATION)
                .filter(event -> Set.of(FeatureUsageAction.CSV_IMPORT_COMPLETED, FeatureUsageAction.CSV_IMPORT_FAILED, FeatureUsageAction.FIRST_IMPORT_COMPLETED).contains(event.getAction()))
                .filter(event -> included(event.getUserId())).sorted(Comparator.comparing((FeatureUsageEvent event) -> occurredAt(event)).reversed()).map(this::operation).toList();
    }

    private boolean included(Long userId) { return users.findById(userId).map(user -> user.getRole() != UserRole.ADMIN && !Boolean.TRUE.equals(user.getAnalyticsExcluded())).orElse(false); }
    private Operation operation(FeatureUsageEvent event) { return new Operation(event, metadata(event.getMetadataJson())); }
    private Map<String, JsonNode> metadata(String json) { try { JsonNode root = json == null ? objectMapper.createObjectNode() : objectMapper.readTree(json); Map<String, JsonNode> values = new HashMap<>(); root.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue())); return values; } catch (Exception ignored) { return Map.of(); } }
    private static LocalDateTime occurredAt(FeatureUsageEvent event) { return event.getOccurredAt() == null ? event.getCreatedAt() : event.getOccurredAt(); }
    private static String mask(User user) { if (user == null || user.getEmail() == null) return "Unknown"; String email = user.getEmail(); int at = email.indexOf('@'); return at <= 1 ? "***" : email.substring(0, 1) + "***" + email.substring(at); }
    private static Double roundPercent(long numerator, long denominator) { return Math.round(numerator * 10000.0 / denominator) / 100.0; }
    private static Window window(Range range) { LocalDate end = LocalDate.now(); return switch (range == null ? Range.LAST_7_DAYS : range) { case TODAY -> new Window(end, end); case LAST_7_DAYS -> new Window(end.minusDays(6), end); case LAST_30_DAYS -> new Window(end.minusDays(29), end); }; }

    public enum Range { TODAY, LAST_7_DAYS, LAST_30_DAYS }
    private record Window(LocalDate start, LocalDate end) { }
    private record Operation(FeatureUsageEvent event, Map<String, JsonNode> metadata) {
        boolean completed() { return event.getAction() == FeatureUsageAction.CSV_IMPORT_COMPLETED; }
        boolean failed() { return event.getAction() == FeatureUsageAction.CSV_IMPORT_FAILED; }
        String stage() { return text("stage", "unknown"); }
        boolean isSaveOperation() { return (completed() || failed()) && "save".equals(stage()); }
        boolean previewFailed() { return failed() && "preview".equals(stage()); }
        String parserSource() { return text("parserSource", "UNKNOWN"); }
        String failureCategory() { return text("failureCategory", "UNKNOWN"); }
        String status() { return completed() ? "COMPLETED" : previewFailed() ? "PREVIEW_FAILED" : "FAILED"; }
        Integer number(String key) { JsonNode value = metadata.get(key); return value != null && value.canConvertToInt() ? value.intValue() : null; }
        long value(String key) { Integer value = number(key); return value == null ? 0 : value; }
        String text(String key, String fallback) { JsonNode value = metadata.get(key); return value != null && value.isTextual() ? value.textValue() : fallback; }
    }
}
