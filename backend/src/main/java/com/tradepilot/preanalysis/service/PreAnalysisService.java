package com.tradepilot.preanalysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.preanalysis.dto.PreAnalysisAccessResponse;
import com.tradepilot.preanalysis.dto.PreAnalysisDtos.*;
import com.tradepilot.preanalysis.dto.TradingPlaybookDto;
import com.tradepilot.preanalysis.dto.TradingPlaybookRequest;
import com.tradepilot.preanalysis.entity.*;
import com.tradepilot.preanalysis.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PreAnalysisService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreAnalysisService.class);
    private static final long MAX_IMAGE_BYTES = 8L * 1024L * 1024L;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp");
    private final FeatureEntitlementService entitlementService;
    private final TradingPlaybookRepository playbookRepository;
    private final AiPreAnalysisRepository analysisRepository;
    private final AiPreAnalysisFeedbackRepository feedbackRepository;
    private final PreAnalysisAiGeneratorService aiGeneratorService;
    private final PreAnalysisDecisionService decisionService;
    private final FeatureUsageService featureUsageService;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;

    public PreAnalysisAccessResponse access(User user) {
        return PreAnalysisAccessResponse.beta(entitlementService.hasEntitlement(user, FeatureEntitlementKey.AI_PRE_ANALYSIS_BETA));
    }

    @Transactional
    public TradingPlaybookDto getActivePlaybook(User user) {
        requireBeta(user);
        return toDto(activePlaybook(user));
    }

    @Transactional
    public TradingPlaybookDto updateActivePlaybook(User user, TradingPlaybookRequest request) {
        requireBeta(user);
        TradingPlaybook playbook = activePlaybook(user);
        apply(playbook, request);
        TradingPlaybook saved = playbookRepository.save(playbook);
        featureUsageService.track(user, FeatureKey.AI_PRE_ANALYSIS, FeatureUsageAction.PRE_ANALYSIS_PLAYBOOK_UPDATED, FeatureUsageEventType.USER_ACTION, null, "TRADING_PLAYBOOK", saved.getId().toString(), "pre_analysis", null);
        return toDto(saved);
    }

    public AnalysisResponse analyze(User user, PreAnalysisMarket market, PreAnalysisTimeframe timeframe, String traderNote, MultipartFile screenshot) {
        long startedAt = System.nanoTime();
        requireBeta(user);
        long entitlementAt = System.nanoTime();
        validateMarketAndTimeframe(market, timeframe);
        long marketValidatedAt = System.nanoTime();
        validateImage(screenshot);
        long imageValidatedAt = System.nanoTime();
        TradingPlaybook playbook = activePlaybook(user);
        long playbookLoadedAt = System.nanoTime();
        byte[] imageBytes;
        try {
            imageBytes = screenshot.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read uploaded screenshot.");
        }
        long imageReadAt = System.nanoTime();
        featureUsageService.track(user, FeatureKey.AI_PRE_ANALYSIS, FeatureUsageAction.PRE_ANALYSIS_SUBMITTED, FeatureUsageEventType.USER_ACTION, null, "TRADING_PLAYBOOK", playbook.getId().toString(), "pre_analysis", metadata(market, timeframe, null, playbook.getId()));
        long submittedTrackedAt = System.nanoTime();
        PreAnalysisAiResult result;
        try {
            boolean transactionActiveBeforeAi = TransactionSynchronizationManager.isActualTransactionActive();
            result = decisionService.normalizeAndValidate(aiGeneratorService.generate(user.getId(), toDto(playbook), market, timeframe, trim(traderNote, 2000), imageBytes, screenshot.getContentType()));
            log.info("pre_analysis_transaction_check userId={} transactionActiveBeforeAi={}", user.getId(), transactionActiveBeforeAi);
        } catch (Exception ex) {
            featureUsageService.track(user, FeatureKey.AI_PRE_ANALYSIS, FeatureUsageAction.PRE_ANALYSIS_FAILED, FeatureUsageEventType.AI_GENERATION, null, "TRADING_PLAYBOOK", playbook.getId().toString(), "pre_analysis", metadata(market, timeframe, null, playbook.getId()));
            throw new IllegalArgumentException("AI Pre-Analysis could not validate the chart response. Please try again.");
        }
        long aiCompletedAt = System.nanoTime();
        AiPreAnalysis analysis = new AiPreAnalysis();
        analysis.setUser(user);
        analysis.setPlaybook(playbook);
        analysis.setMarket(market);
        analysis.setTimeframe(timeframe);
        analysis.setImageReference(UUID.randomUUID().toString());
        analysis.setImageContentType(screenshot.getContentType());
        analysis.setImageBytes(imageBytes);
        analysis.setTraderNote(trim(traderNote, 2000));
        analysis.setSetupStatus(result.setupStatus());
        analysis.setBias(result.directionalContext().bias());
        analysis.setSetupAlignmentScore(result.setupAlignmentScore());
        analysis.setStructureQualityScore(result.structureQualityScore());
        analysis.setAnalysisJson(write(result));
        analysis.setModelName(openAiProperties.getModel());
        analysis.setPromptVersion(PreAnalysisPromptBuilder.PROMPT_VERSION);
        AiPreAnalysis saved = analysisRepository.save(analysis);
        long persistedAt = System.nanoTime();
        trackGenerated(user, saved);
        long telemetryAt = System.nanoTime();
        log.info("pre_analysis_request_timing userId={} market={} timeframe={} setupStatus={} alignmentScore={} entitlementMs={} marketValidationMs={} imageValidationMs={} playbookLoadMs={} imageReadMs={} submittedTelemetryMs={} aiAndNormalizeMs={} persistenceMs={} generatedTelemetryMs={} totalMs={}", user.getId(), market, timeframe, saved.getSetupStatus(), saved.getSetupAlignmentScore(), ms(startedAt, entitlementAt), ms(entitlementAt, marketValidatedAt), ms(marketValidatedAt, imageValidatedAt), ms(imageValidatedAt, playbookLoadedAt), ms(playbookLoadedAt, imageReadAt), ms(imageReadAt, submittedTrackedAt), ms(submittedTrackedAt, aiCompletedAt), ms(aiCompletedAt, persistedAt), ms(persistedAt, telemetryAt), ms(startedAt, telemetryAt));
        return toResponse(saved, result, null);
    }

    @Transactional(readOnly = true)
    public List<AnalysisSummaryResponse> history(User user) {
        requireBeta(user);
        return analysisRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 20)).stream()
                .map(analysis -> {
                    Integer rating = feedbackRepository.findByAnalysisAndUser(analysis, user).map(AiPreAnalysisFeedback::getRating).orElse(null);
                    return new AnalysisSummaryResponse(analysis.getId(), analysis.getMarket(), analysis.getTimeframe(), PreAnalysisPromptBuilder.displayTimeframe(analysis.getTimeframe()), analysis.getSetupStatus(), analysis.getBias(), analysis.getSetupAlignmentScore(), rating, analysis.getCreatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(User user, Long analysisId) {
        requireBeta(user);
        AiPreAnalysis analysis = findOwned(user, analysisId);
        return toResponse(analysis, read(analysis.getAnalysisJson()), feedbackRepository.findByAnalysisAndUser(analysis, user).map(this::toFeedback).orElse(null));
    }

    @Transactional(readOnly = true)
    public ImageResource image(User user, Long analysisId) {
        requireBeta(user);
        AiPreAnalysis analysis = findOwned(user, analysisId);
        return new ImageResource(analysis.getImageContentType(), analysis.getImageBytes());
    }

    @Transactional
    public FeedbackResponse feedback(User user, Long analysisId, FeedbackRequest request) {
        requireBeta(user);
        AiPreAnalysis analysis = findOwned(user, analysisId);
        int rating = request.rating() == null ? 0 : request.rating();
        if (rating != 1 && rating != -1) {
            throw new IllegalArgumentException("Feedback rating must be 1 or -1.");
        }
        AiPreAnalysisFeedback feedback = feedbackRepository.findByAnalysisAndUser(analysis, user).orElseGet(AiPreAnalysisFeedback::new);
        feedback.setAnalysis(analysis);
        feedback.setUser(user);
        feedback.setRating(rating);
        feedback.setReasonCode(rating < 0 ? request.reasonCode() : null);
        feedback.setComment(trim(request.comment(), 1000));
        AiPreAnalysisFeedback saved = feedbackRepository.save(feedback);
        featureUsageService.track(user, FeatureKey.AI_PRE_ANALYSIS, FeatureUsageAction.PRE_ANALYSIS_FEEDBACK_SUBMITTED, FeatureUsageEventType.USER_ACTION, null, "AI_PRE_ANALYSIS", analysis.getId().toString(), "pre_analysis", metadata(analysis.getMarket(), analysis.getTimeframe(), analysis.getSetupStatus(), analysis.getPlaybook().getId()));
        return toFeedback(saved);
    }

    private TradingPlaybook activePlaybook(User user) {
        return playbookRepository.findFirstByUserAndActiveTrueOrderByUpdatedAtDesc(user).orElseGet(() -> playbookRepository.save(defaultFounderPlaybook(user)));
    }

    private TradingPlaybook defaultFounderPlaybook(User user) {
        TradingPlaybook playbook = new TradingPlaybook();
        playbook.setUser(user);
        playbook.setName("Break + Acceptance + Displacement + FVG/IFVG Retracement");
        playbook.setDescription("Personal private-beta playbook for checking whether ES/NQ charts match the committed break, acceptance, displacement, FVG/IFVG retracement sequence.");
        playbook.setMarkets(writeList(List.of("ES", "NQ")));
        playbook.setContextTimeframes(writeList(List.of("15m")));
        playbook.setSetupTimeframes(writeList(List.of("5m")));
        playbook.setExecutionTimeframes(writeList(List.of("1m")));
        playbook.setSetupSequence(writeList(List.of(
                "BREAK: Price decisively breaks a meaningful structural/reference level. Do not count a wick-only breach.",
                "ACCEPTANCE: Prefer a 5m or 15m body close beyond the level. Wick-and-immediate-reclaim does not count.",
                "DISPLACEMENT: Require strong directional expansion with momentum and preferably FVG/IFVG creation.",
                "FVG / IFVG: Displacement should create a clear FVG/IFVG or defined structural retracement area.",
                "RETRACEMENT: Do not chase. Wait for retracement into FVG, IFVG, broken structure, or closely related PD array.",
                "CONFIRMATION: At retracement, require failed reclaim, LH/HL, displacement back in intended direction, or rejection.",
                "ENTRY: Only valid after the full sequence is sufficiently satisfied."
        )));
        playbook.setEntryRules(writeList(List.of("Do not anticipate breaks.", "Do not chase displacement.", "Correct bias does not equal valid entry.", "If no clean retracement occurs, no trade.")));
        playbook.setInvalidationRules(writeList(List.of("Structural invalidation determines stop.", "Invalidated if price reclaims the structure that should have held.")));
        playbook.setTargetRules(writeList(List.of("Targets should reference next meaningful liquidity or opposing PD array.")));
        playbook.setAvoidRules(writeList(List.of("Avoid overlapping/choppy/two-sided PA.", "Avoid revenge re-entry after a stopped trade unless a completely new setup forms.")));
        playbook.setCustomNotes("Founder private-beta playbook. NO TRADE is valid when setup quality is unclear.");
        playbook.setActive(true);
        return playbook;
    }

    private void apply(TradingPlaybook playbook, TradingPlaybookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Playbook request is required.");
        }
        playbook.setName(required(request.name(), "Playbook name is required.", 160));
        playbook.setDescription(trim(request.description(), 5000));
        playbook.setMarkets(writeList(request.markets()));
        playbook.setContextTimeframes(writeList(request.contextTimeframes()));
        playbook.setSetupTimeframes(writeList(request.setupTimeframes()));
        playbook.setExecutionTimeframes(writeList(request.executionTimeframes()));
        playbook.setSetupSequence(writeList(request.setupSequence()));
        playbook.setEntryRules(writeList(request.entryRules()));
        playbook.setInvalidationRules(writeList(request.invalidationRules()));
        playbook.setTargetRules(writeList(request.targetRules()));
        playbook.setAvoidRules(writeList(request.avoidRules()));
        playbook.setCustomNotes(trim(request.customNotes(), 5000));
        playbook.setActive(true);
    }

    private void requireBeta(User user) {
        entitlementService.requireEntitlement(user, FeatureEntitlementKey.AI_PRE_ANALYSIS_BETA);
    }

    private void validateMarketAndTimeframe(PreAnalysisMarket market, PreAnalysisTimeframe timeframe) {
        if (market == null || timeframe == null) {
            throw new IllegalArgumentException("Market and timeframe are required.");
        }
    }

    private void validateImage(MultipartFile screenshot) {
        if (screenshot == null || screenshot.isEmpty()) {
            throw new IllegalArgumentException("Chart screenshot is required.");
        }
        if (screenshot.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Chart screenshot must be 8MB or smaller.");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(screenshot.getContentType())) {
            throw new IllegalArgumentException("Chart screenshot must be PNG, JPEG, or WebP.");
        }
    }

    private AiPreAnalysis findOwned(User user, Long analysisId) {
        return analysisRepository.findByIdAndUser(analysisId, user).orElseThrow(() -> new ResourceNotFoundException("Pre-analysis not found"));
    }

    private TradingPlaybookDto toDto(TradingPlaybook playbook) {
        return new TradingPlaybookDto(playbook.getId(), playbook.getName(), playbook.getDescription(), readList(playbook.getMarkets()), readList(playbook.getContextTimeframes()), readList(playbook.getSetupTimeframes()), readList(playbook.getExecutionTimeframes()), readList(playbook.getSetupSequence()), readList(playbook.getEntryRules()), readList(playbook.getInvalidationRules()), readList(playbook.getTargetRules()), readList(playbook.getAvoidRules()), playbook.getCustomNotes(), playbook.isActive(), playbook.getCreatedAt(), playbook.getUpdatedAt());
    }

    private AnalysisResponse toResponse(AiPreAnalysis analysis, PreAnalysisAiResult result, FeedbackResponse feedback) {
        return new AnalysisResponse(analysis.getId(), analysis.getMarket(), analysis.getTimeframe(), PreAnalysisPromptBuilder.displayTimeframe(analysis.getTimeframe()), analysis.getPlaybook().getId(), analysis.getPlaybook().getName(), "/pre-analysis/" + analysis.getId() + "/image", analysis.getTraderNote(), result.setupStatus(), result.directionalContext().bias(), result.setupAlignmentScore(), result.structureQualityScore(), result, feedback, analysis.getCreatedAt());
    }

    private FeedbackResponse toFeedback(AiPreAnalysisFeedback feedback) {
        return new FeedbackResponse(feedback.getId(), feedback.getRating(), feedback.getReasonCode(), feedback.getComment(), feedback.getCreatedAt());
    }

    private PreAnalysisAiResult read(String json) {
        try {
            PreAnalysisAiResult result = objectMapper.readValue(json, PreAnalysisAiResult.class);
            return decisionService.normalizeAndValidate(result);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Stored AI Pre-Analysis response is invalid.");
        }
    }

    private String write(PreAnalysisAiResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to persist AI Pre-Analysis response.");
        }
    }

    private String writeList(List<String> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value.stream().filter(item -> item != null && !item.isBlank()).map(item -> trim(item, 2000)).toList());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize playbook rules.");
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String required(String value, String message, int max) {
        String trimmed = trim(value, max);
        if (trimmed == null || trimmed.isBlank()) throw new IllegalArgumentException(message);
        return trimmed;
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private String metadata(PreAnalysisMarket market, PreAnalysisTimeframe timeframe, SetupStatus status, Long playbookId) {
        return "{\"market\":\"" + market + "\",\"timeframe\":\"" + PreAnalysisPromptBuilder.displayTimeframe(timeframe) + "\",\"setupStatus\":\"" + (status == null ? "" : status.name()) + "\",\"playbookId\":" + playbookId + "}";
    }

    private void trackGenerated(User user, AiPreAnalysis analysis) {
        featureUsageService.track(user, FeatureKey.AI_PRE_ANALYSIS, FeatureUsageAction.PRE_ANALYSIS_GENERATED, FeatureUsageEventType.AI_GENERATION, null, "AI_PRE_ANALYSIS", analysis.getId().toString(), "pre_analysis", metadata(analysis.getMarket(), analysis.getTimeframe(), analysis.getSetupStatus(), analysis.getPlaybook().getId()));
        FeatureUsageAction action = switch (analysis.getSetupStatus()) {
            case ENTRY_VALID -> FeatureUsageAction.PRE_ANALYSIS_ENTRY_VALID;
            case INVALIDATED -> FeatureUsageAction.PRE_ANALYSIS_INVALIDATED;
            case NO_TRADE -> FeatureUsageAction.PRE_ANALYSIS_NO_TRADE;
            default -> FeatureUsageAction.PRE_ANALYSIS_WAIT;
        };
        featureUsageService.track(user, FeatureKey.AI_PRE_ANALYSIS, action, FeatureUsageEventType.USER_ACTION, null, "AI_PRE_ANALYSIS", analysis.getId().toString(), "pre_analysis", metadata(analysis.getMarket(), analysis.getTimeframe(), analysis.getSetupStatus(), analysis.getPlaybook().getId()));
    }

    public record ImageResource(String contentType, byte[] bytes) {}

    private long ms(long start, long end) {
        return Duration.ofNanos(end - start).toMillis();
    }

    public PreAnalysisService(FeatureEntitlementService entitlementService, TradingPlaybookRepository playbookRepository, AiPreAnalysisRepository analysisRepository, AiPreAnalysisFeedbackRepository feedbackRepository, PreAnalysisAiGeneratorService aiGeneratorService, PreAnalysisDecisionService decisionService, FeatureUsageService featureUsageService, ObjectMapper objectMapper, OpenAiProperties openAiProperties) {
        this.entitlementService = entitlementService;
        this.playbookRepository = playbookRepository;
        this.analysisRepository = analysisRepository;
        this.feedbackRepository = feedbackRepository;
        this.aiGeneratorService = aiGeneratorService;
        this.decisionService = decisionService;
        this.featureUsageService = featureUsageService;
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
    }
}
