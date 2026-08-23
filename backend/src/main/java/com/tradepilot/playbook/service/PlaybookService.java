package com.tradepilot.playbook.service;

import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.playbook.dto.*;
import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.entity.PlaybookMilestone;
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.*;
import com.tradepilot.playbook.repository.*;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlaybookService {
    private final FeatureAccessService featureAccessService;
    private final UsageLimitService usageLimitService;
    private final TradingAccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final PlaybookSetupRepository setupRepository;
    private final PlaybookChecklistItemRepository checklistRepository;
    private final TradePlaybookAssignmentRepository assignmentRepository;
    private final PlaybookMilestoneRepository milestoneRepository;
    private final PlaybookJsonService jsonService;
    private final PlaybookComplianceService complianceService;
    private final PlaybookAnalysisService analysisService;

    public PlaybookService(FeatureAccessService featureAccessService, UsageLimitService usageLimitService, TradingAccountRepository accountRepository, TradeRepository tradeRepository, PlaybookSetupRepository setupRepository, PlaybookChecklistItemRepository checklistRepository, TradePlaybookAssignmentRepository assignmentRepository, PlaybookMilestoneRepository milestoneRepository, PlaybookJsonService jsonService, PlaybookComplianceService complianceService, PlaybookAnalysisService analysisService) {
        this.featureAccessService = featureAccessService;
        this.usageLimitService = usageLimitService;
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.setupRepository = setupRepository;
        this.checklistRepository = checklistRepository;
        this.assignmentRepository = assignmentRepository;
        this.milestoneRepository = milestoneRepository;
        this.jsonService = jsonService;
        this.complianceService = complianceService;
        this.analysisService = analysisService;
    }

    @Transactional
    public List<PlaybookSetupSummaryResponse> list(User user, Long accountId, PlaybookStatus status, String search, int page, int size) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 50));
        String normalizedSearch = normalizedSearch(search);
        return (normalizedSearch == null
                ? setupRepository.search(user.getId(), accountId, status, pageRequest)
                : setupRepository.searchByName(user.getId(), accountId, status, "%" + normalizedSearch + "%", pageRequest))
                .map(setup -> summary(user, setup))
                .toList();
    }

    @Transactional
    public PlaybookOverviewResponse overview(User user, Long accountId) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        List<PlaybookSetupSummaryResponse> recent = list(user, accountId, null, null, 0, 12);
        PlaybookSetupSummaryResponse best = recent.stream().max(Comparator.comparing(s -> n(s.totalR()))).orElse(null);
        PlaybookSetupSummaryResponse compliance = recent.stream().max(Comparator.comparing(s -> s.complianceScore() == null ? 0 : s.complianceScore())).orElse(null);
        PlaybookSetupSummaryResponse health = recent.stream().max(Comparator.comparing(s -> s.healthScore() == null ? 0 : s.healthScore())).orElse(null);
        PlaybookSetupSummaryResponse attention = recent.stream().min(Comparator.comparing(s -> s.healthScore() == null ? 100 : s.healthScore())).orElse(null);
        return new PlaybookOverviewResponse(
                setupRepository.countByUser_IdAndStatusNot(user.getId(), PlaybookStatus.ARCHIVED),
                setupRepository.countByUser_IdAndStatus(user.getId(), PlaybookStatus.DRAFT),
                setupRepository.countByUser_IdAndStatus(user.getId(), PlaybookStatus.TESTING),
                setupRepository.countByUser_IdAndStatus(user.getId(), PlaybookStatus.VALIDATED),
                setupRepository.countByUser_IdAndStatus(user.getId(), PlaybookStatus.ARCHIVED),
                best, compliance, health, attention, recent, recentMilestones(user)
        );
    }

    @Transactional
    public PlaybookSetupDetailResponse get(User user, Long setupId) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        PlaybookSetup setup = find(user, setupId);
        return detail(user, setup);
    }

    @Transactional
    public PlaybookSetupDetailResponse create(User user, PlaybookSetupRequest request) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        validate(request, true);
        PlaybookSetup setup = new PlaybookSetup();
        setup.setUser(user);
        apply(setup, user, request);
        PlaybookSetup saved = setupRepository.save(setup);
        saveChecklist(saved, request.checklistItems());
        award(user, saved, "FIRST_PLAYBOOK_CREATED", List.of("Created setup: " + saved.getName()));
        return detail(user, saved);
    }

    @Transactional
    public PlaybookSetupDetailResponse update(User user, Long setupId, PlaybookSetupRequest request) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        validate(request, false);
        PlaybookSetup setup = find(user, setupId);
        apply(setup, user, request);
        PlaybookSetup saved = setupRepository.save(setup);
        checklistRepository.deleteBySetup_Id(saved.getId());
        saveChecklist(saved, request.checklistItems());
        return detail(user, saved);
    }

    @Transactional
    public PlaybookSetupDetailResponse archive(User user, Long setupId) {
        PlaybookSetup setup = find(user, setupId);
        setup.setStatus(PlaybookStatus.ARCHIVED);
        setup.setArchivedAt(LocalDateTime.now());
        return detail(user, setupRepository.save(setup));
    }

    @Transactional
    public PlaybookSetupDetailResponse restore(User user, Long setupId) {
        PlaybookSetup setup = find(user, setupId);
        setup.setStatus(PlaybookStatus.TESTING);
        setup.setArchivedAt(null);
        return detail(user, setupRepository.save(setup));
    }

    @Transactional
    public PlaybookSetupDetailResponse duplicate(User user, Long setupId) {
        PlaybookSetup original = find(user, setupId);
        PlaybookSetup copy = new PlaybookSetup();
        copy.setUser(user);
        copy.setAccount(original.getAccount());
        copy.setName(original.getName() + " Copy");
        copy.setShortDescription(original.getShortDescription());
        copy.setDescription(original.getDescription());
        copy.setCategory(original.getCategory());
        copy.setStatus(PlaybookStatus.DRAFT);
        copy.setDirection(original.getDirection());
        copy.setInstrumentsJson(original.getInstrumentsJson());
        copy.setSessionsJson(original.getSessionsJson());
        copy.setTimeframesJson(original.getTimeframesJson());
        copy.setTagsJson(original.getTagsJson());
        copy.setMarketContextJson(original.getMarketContextJson());
        copy.setRiskRulesJson(original.getRiskRulesJson());
        copy.setManagementRulesJson(original.getManagementRulesJson());
        copy.setPostTradeRulesJson(original.getPostTradeRulesJson());
        PlaybookSetup saved = setupRepository.save(copy);
        List<PlaybookChecklistItemDto> items = checklistRepository.findBySetup_IdOrderByChecklistTypeAscDisplayOrderAscIdAsc(original.getId()).stream().map(this::itemDto).toList();
        saveChecklist(saved, items);
        return detail(user, saved);
    }

    @Transactional
    public PlaybookTradeAssignmentResponse linkTrade(User user, Long setupId, Long tradeId, PlaybookTradeAssignmentRequest request) {
        PlaybookSetup setup = find(user, setupId);
        if (setup.getStatus() == PlaybookStatus.ARCHIVED) throw new IllegalArgumentException("Archived setups cannot be linked until restored.");
        Trade trade = tradeRepository.findByIdAndUser_Id(tradeId, user.getId()).orElseThrow(() -> new IllegalArgumentException("Trade not found."));
        if (setup.getAccount() != null && trade.getAccount() != null && !Objects.equals(setup.getAccount().getId(), trade.getAccount().getId())) {
            throw new IllegalArgumentException("Trade does not belong to this setup account.");
        }
        TradePlaybookAssignment assignment = assignmentRepository.findByTrade_IdAndUser_Id(tradeId, user.getId()).orElseGet(TradePlaybookAssignment::new);
        assignment.setUser(user);
        assignment.setAccount(trade.getAccount());
        assignment.setTrade(trade);
        assignment.setSetup(setup);
        applyAssignment(setup, assignment, request);
        TradePlaybookAssignment saved = assignmentRepository.save(assignment);
        award(user, setup, "FIRST_SETUP_LINKED", List.of("Linked trade " + trade.getId() + " to " + setup.getName()));
        long count = assignmentRepository.countBySetup_Id(setupId);
        if (count >= 10) award(user, setup, "FIRST_10_LINKED_TRADES", List.of(count + " linked trades"));
        if (count >= 30) award(user, setup, "FIRST_30_LINKED_TRADES", List.of(count + " linked trades"));
        return assignmentDto(saved);
    }

    @Transactional
    public List<PlaybookTradeAssignmentResponse> bulkLink(User user, Long setupId, BulkLinkTradesRequest request) {
        if (request.tradeIds() == null) return List.of();
        return request.tradeIds().stream().limit(100).map(id -> linkTrade(user, setupId, id, request.assignment())).toList();
    }

    @Transactional
    public void unlinkTrade(User user, Long setupId, Long tradeId) {
        find(user, setupId);
        assignmentRepository.deleteBySetup_IdAndTrade_IdAndUser_Id(setupId, tradeId, user.getId());
    }

    @Transactional(readOnly = true)
    public List<PlaybookTradeAssignmentResponse> linkedTrades(User user, Long setupId) {
        find(user, setupId);
        return assignmentRepository.findBySetup_IdOrderByTrade_TradeDateDescTrade_EntryTimeDescIdDesc(setupId).stream().map(this::assignmentDto).toList();
    }

    @Transactional
    public PlaybookTradeAssignmentResponse updateAssignment(User user, Long assignmentId, PlaybookTradeAssignmentRequest request) {
        TradePlaybookAssignment assignment = assignmentRepository.findByIdAndUser_Id(assignmentId, user.getId()).orElseThrow(() -> new IllegalArgumentException("Playbook assignment not found."));
        applyAssignment(assignment.getSetup(), assignment, request);
        return assignmentDto(assignmentRepository.save(assignment));
    }

    @Transactional
    public PlaybookDraftResponse draftFromDescription(User user, PlaybookDraftRequest request) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        if (request.description() == null || request.description().isBlank()) throw new IllegalArgumentException("Description is required.");
        usageLimitService.consumeCredits(user, Feature.PLAYBOOK, 1, null, "PLAYBOOK_DRAFT_DESCRIPTION");
        String text = request.description();
        PlaybookCategory category = text.toUpperCase().contains("FVG") ? PlaybookCategory.FVG : PlaybookCategory.CUSTOM;
        PlaybookSetupRequest draft = new PlaybookSetupRequest(request.accountId(), suggestedName(text), text.length() > 180 ? text.substring(0, 180) : text, text, category, PlaybookStatus.DRAFT, directionFrom(text), List.of(), List.of(), List.of(), List.of("AI_DRAFT"), List.of("User-described conditions must be reviewed before saving."), List.of("Risk must be within planned limits."), List.of("Manage trade according to documented stop and target."), List.of("Review notes, emotion, and confidence after the trade."), "book", "teal", null, defaultChecklist(text));
        return new PlaybookDraftResponse(draft, List.of("Draft structure was derived from the user description.", "No profitability or validation claim was made."), 1, "Review and edit this draft before saving.");
    }

    @Transactional
    public PlaybookDraftResponse draftFromTrades(User user, PlaybookDraftRequest request) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        List<Long> ids = request.tradeIds() == null ? List.of() : request.tradeIds().stream().distinct().limit(50).toList();
        List<Trade> trades = tradeRepository.findAllById(ids).stream().filter(t -> Objects.equals(t.getUser().getId(), user.getId())).toList();
        if (trades.size() < 5) {
            return new PlaybookDraftResponse(null, List.of("At least 5 valid selected trades are required."), 0, "No credits consumed.");
        }
        usageLimitService.consumeCredits(user, Feature.PLAYBOOK, 2, null, "PLAYBOOK_DRAFT_TRADES");
        String symbol = trades.get(0).getSymbol();
        PlaybookSetupRequest draft = new PlaybookSetupRequest(request.accountId(), "Draft from " + trades.size() + " selected trades", "Review common conditions from selected trades.", "Draft generated from deterministic selected-trade facts. Review chart-specific rules manually.", PlaybookCategory.CUSTOM, PlaybookStatus.DRAFT, PlaybookDirection.BOTH, List.of(symbol), List.of(), List.of(), List.of("TRADE_DRAFT"), List.of("Common timing and instrument patterns require user review."), List.of("Use account risk settings for 1R."), List.of("Document management rules before validation."), List.of("Grade setup and execution after each linked trade."), "layers", "green", null, defaultChecklist("selected trades"));
        return new PlaybookDraftResponse(draft, List.of("Based on " + trades.size() + " user-owned trades.", "No chart structure was inferred from screenshots."), 2, "Review and edit this draft before saving.");
    }

    private PlaybookSetupDetailResponse detail(User user, PlaybookSetup setup) {
        List<PlaybookChecklistItemDto> items = checklistRepository.findBySetup_IdOrderByChecklistTypeAscDisplayOrderAscIdAsc(setup.getId()).stream().map(this::itemDto).toList();
        PlaybookAnalysisResponse analysis = analysisService.build(user, setup, true);
        return new PlaybookSetupDetailResponse(setup.getId(), accountId(setup), accountName(setup), setup.getName(), setup.getShortDescription(), setup.getDescription(), setup.getCategory(), setup.getStatus(), setup.getDirection(), jsonService.readStringList(setup.getInstrumentsJson()), jsonService.readStringList(setup.getSessionsJson()), jsonService.readStringList(setup.getTimeframesJson()), jsonService.readStringList(setup.getTagsJson()), jsonService.readStringList(setup.getMarketContextJson()), jsonService.readStringList(setup.getRiskRulesJson()), jsonService.readStringList(setup.getManagementRulesJson()), jsonService.readStringList(setup.getPostTradeRulesJson()), setup.getIconKey(), setup.getColorKey(), setup.getImageUrl(), setup.getVersionNumber(), setup.getCreatedAt(), setup.getUpdatedAt(), setup.getArchivedAt(), items, analysis);
    }

    private PlaybookSetupSummaryResponse summary(User user, PlaybookSetup setup) {
        PlaybookAnalysisResponse analysis = analysisService.build(user, setup, true);
        return new PlaybookSetupSummaryResponse(setup.getId(), accountId(setup), accountName(setup), setup.getName(), setup.getShortDescription(), setup.getCategory(), setup.getStatus(), setup.getDirection(), jsonService.readStringList(setup.getInstrumentsJson()), jsonService.readStringList(setup.getSessionsJson()), jsonService.readStringList(setup.getTagsJson()), analysis.metrics().totalLinkedTrades(), analysis.metrics().totalR(), analysis.metrics().netPnl(), analysis.metrics().winRate(), analysis.metrics().profitFactor(), averageCompliance(setup.getId()), analysis.healthScore().score(), analysis.healthScore().confidence(), analysis.metrics().lastUsedDate(), analysis.generatedAt(), analysis.aiReviewStale());
    }

    private Integer averageCompliance(Long setupId) {
        List<TradePlaybookAssignment> assignments = assignmentRepository.findBySetup_IdOrderByTrade_TradeDateDescTrade_EntryTimeDescIdDesc(setupId);
        return assignments.stream().map(TradePlaybookAssignment::getComplianceScore).filter(Objects::nonNull).mapToInt(Integer::intValue).average().stream().mapToInt(v -> (int) Math.round(v)).findFirst().orElse(0);
    }

    private void apply(PlaybookSetup setup, User user, PlaybookSetupRequest request) {
        setup.setAccount(resolveAccount(user, request.accountId()));
        setup.setName(request.name().trim());
        setup.setShortDescription(request.shortDescription());
        setup.setDescription(request.description());
        setup.setCategory(request.category() == null ? PlaybookCategory.CUSTOM : request.category());
        setup.setStatus(request.status() == null ? PlaybookStatus.DRAFT : request.status());
        setup.setDirection(request.direction() == null ? PlaybookDirection.BOTH : request.direction());
        setup.setInstrumentsJson(jsonService.write(request.instruments()));
        setup.setSessionsJson(jsonService.write(request.sessions()));
        setup.setTimeframesJson(jsonService.write(request.timeframes()));
        setup.setTagsJson(jsonService.write(request.tags()));
        setup.setMarketContextJson(jsonService.write(request.marketContext()));
        setup.setRiskRulesJson(jsonService.write(request.riskRules()));
        setup.setManagementRulesJson(jsonService.write(request.managementRules()));
        setup.setPostTradeRulesJson(jsonService.write(request.postTradeRules()));
        setup.setIconKey(request.iconKey());
        setup.setColorKey(request.colorKey());
        setup.setImageUrl(request.imageUrl());
        if (setup.getStatus() == PlaybookStatus.ARCHIVED && setup.getArchivedAt() == null) setup.setArchivedAt(LocalDateTime.now());
    }

    private void validate(PlaybookSetupRequest request, boolean create) {
        if (request == null || request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("Setup name is required.");
        if (request.name().length() > 120) throw new IllegalArgumentException("Setup name must be 120 characters or less.");
        PlaybookStatus status = request.status() == null ? PlaybookStatus.DRAFT : request.status();
        long activeEntry = request.checklistItems() == null ? 0 : request.checklistItems().stream().filter(i -> i.checklistType() == ChecklistType.ENTRY && !Boolean.FALSE.equals(i.active())).count();
        long invalidation = request.checklistItems() == null ? 0 : request.checklistItems().stream().filter(i -> i.checklistType() == ChecklistType.INVALIDATION && !Boolean.FALSE.equals(i.active())).count();
        long risk = request.checklistItems() == null ? 0 : request.checklistItems().stream().filter(i -> i.checklistType() == ChecklistType.RISK && !Boolean.FALSE.equals(i.active())).count();
        if (status != PlaybookStatus.DRAFT && activeEntry == 0) throw new IllegalArgumentException("Testing or validated setups need at least one entry rule.");
        if (status == PlaybookStatus.VALIDATED && (invalidation == 0 || risk == 0)) throw new IllegalArgumentException("Validated setups need invalidation and risk rules.");
    }

    private void saveChecklist(PlaybookSetup setup, List<PlaybookChecklistItemDto> items) {
        if (items == null) return;
        int order = 0;
        for (PlaybookChecklistItemDto dto : items) {
            if (dto.label() == null || dto.label().isBlank()) continue;
            PlaybookChecklistItem item = new PlaybookChecklistItem();
            item.setSetup(setup);
            item.setChecklistType(dto.checklistType() == null ? ChecklistType.ENTRY : dto.checklistType());
            item.setLabel(dto.label().trim());
            item.setDescription(dto.description());
            item.setRequired(dto.required() == null || dto.required());
            item.setWeight(dto.weight() == null || dto.weight() <= 0 ? 1 : dto.weight());
            item.setDisplayOrder(dto.displayOrder() == null ? order++ : dto.displayOrder());
            item.setActive(dto.active() == null || dto.active());
            item.setExpectedValueType(dto.expectedValueType());
            item.setExpectedValueJson(jsonService.write(dto.expectedValue()));
            checklistRepository.save(item);
        }
    }

    private void applyAssignment(PlaybookSetup setup, TradePlaybookAssignment assignment, PlaybookTradeAssignmentRequest request) {
        Map<Long, String> results = request == null || request.checklistResults() == null ? Map.of() : request.checklistResults();
        List<PlaybookChecklistItem> items = checklistRepository.findBySetup_IdOrderByChecklistTypeAscDisplayOrderAscIdAsc(setup.getId());
        PlaybookComplianceResult compliance = complianceService.score(items, results);
        assignment.setSetupQualityGrade(request == null || request.setupQualityGrade() == null ? SetupQualityGrade.UNGRADED : request.setupQualityGrade());
        assignment.setExecutionQualityGrade(request == null || request.executionQualityGrade() == null ? ExecutionQualityGrade.UNGRADED : request.executionQualityGrade());
        assignment.setChecklistResultsJson(jsonService.write(results));
        assignment.setRuleViolationsJson(jsonService.write(request == null ? null : request.ruleViolations()));
        assignment.setAssignmentSource(request == null || request.assignmentSource() == null ? AssignmentSource.MANUAL : request.assignmentSource());
        assignment.setAssignmentConfidence(request == null ? null : request.assignmentConfidence());
        assignment.setReviewedByUser(request != null && Boolean.TRUE.equals(request.reviewedByUser()));
        assignment.setNotes(request == null ? null : request.notes());
        assignment.setComplianceScore(compliance.score());
        assignment.setComplianceConfidence(compliance.confidence());
    }

    private PlaybookTradeAssignmentResponse assignmentDto(TradePlaybookAssignment assignment) {
        Trade trade = assignment.getTrade();
        return new PlaybookTradeAssignmentResponse(assignment.getId(), trade.getId(), assignment.getSetup().getId(), assignment.getSetup().getName(), trade.getTradeDate(), trade.getSymbol(), trade.getSide(), trade.getPnlAmount(), trade.getPnlR(), assignment.getSetupQualityGrade(), assignment.getExecutionQualityGrade(), assignment.getComplianceScore(), assignment.getComplianceConfidence(), jsonService.readChecklistResults(assignment.getChecklistResultsJson()), jsonService.readStringList(assignment.getRuleViolationsJson()).toArray(String[]::new), assignment.getAssignmentSource(), assignment.getReviewedByUser(), assignment.getNotes());
    }

    private PlaybookChecklistItemDto itemDto(PlaybookChecklistItem item) {
        return new PlaybookChecklistItemDto(item.getId(), item.getChecklistType(), item.getLabel(), item.getDescription(), item.getRequired(), item.getWeight(), item.getDisplayOrder(), item.getActive(), item.getExpectedValueType(), null);
    }

    private List<PlaybookMilestoneResponse> recentMilestones(User user) {
        return milestoneRepository.findTop10ByUser_IdOrderByEarnedDateDescCreatedAtDesc(user.getId()).stream().map(m -> new PlaybookMilestoneResponse(m.getId(), m.getSetup() == null ? null : m.getSetup().getId(), m.getMilestoneType(), m.getEarnedDate(), jsonService.readStringList(m.getEvidenceJson()))).toList();
    }

    private void award(User user, PlaybookSetup setup, String type, List<String> evidence) {
        if (milestoneRepository.existsByUser_IdAndSetup_IdAndMilestoneType(user.getId(), setup.getId(), type)) return;
        PlaybookMilestone milestone = new PlaybookMilestone();
        milestone.setUser(user);
        milestone.setAccountId(accountId(setup));
        milestone.setSetup(setup);
        milestone.setMilestoneType(type);
        milestone.setEarnedDate(LocalDate.now());
        milestone.setEvidenceJson(jsonService.write(evidence));
        milestoneRepository.save(milestone);
    }

    private PlaybookSetup find(User user, Long setupId) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        return setupRepository.findByIdAndUser_Id(setupId, user.getId()).orElseThrow(() -> new IllegalArgumentException("Playbook setup not found."));
    }

    private TradingAccount resolveAccount(User user, Long accountId) {
        if (accountId == null) return null;
        return accountRepository.findByIdAndUser_Id(accountId, user.getId()).orElseThrow(() -> new IllegalArgumentException("Trading account not found."));
    }

    private Long accountId(PlaybookSetup setup) {
        return setup.getAccount() == null ? null : setup.getAccount().getId();
    }

    private String accountName(PlaybookSetup setup) {
        return setup.getAccount() == null ? null : setup.getAccount().getName();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizedSearch(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private java.math.BigDecimal n(java.math.BigDecimal value) {
        return value == null ? java.math.BigDecimal.ZERO : value;
    }

    private String suggestedName(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 48) return normalized;
        return normalized.substring(0, 48).trim();
    }

    private PlaybookDirection directionFrom(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("long") && !lower.contains("short")) return PlaybookDirection.LONG;
        if (lower.contains("short") && !lower.contains("long")) return PlaybookDirection.SHORT;
        return PlaybookDirection.BOTH;
    }

    private List<PlaybookChecklistItemDto> defaultChecklist(String source) {
        return List.of(
                new PlaybookChecklistItemDto(null, ChecklistType.CONTEXT, "Market context matches the setup description", "Review this assumption before saving.", true, 1, 0, true, null, null),
                new PlaybookChecklistItemDto(null, ChecklistType.ENTRY, "Entry condition is present", "Confirm this with actual chart review.", true, 1, 1, true, null, null),
                new PlaybookChecklistItemDto(null, ChecklistType.RISK, "Risk is within planned limits", "Use configured account risk or trade-level risk.", true, 1, 2, true, null, null),
                new PlaybookChecklistItemDto(null, ChecklistType.POST_TRADE, "Trade is reviewed after exit", "Capture notes, emotion, and confidence.", false, 1, 3, true, null, null)
        );
    }
}
