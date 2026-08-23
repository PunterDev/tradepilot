package com.tradepilot.playbook.controller;

import com.tradepilot.billing.Feature;
import com.tradepilot.entity.User;
import com.tradepilot.config.FeatureFlagService;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.playbook.dto.*;
import com.tradepilot.playbook.enums.PlaybookStatus;
import com.tradepilot.playbook.service.PlaybookAnalysisService;
import com.tradepilot.playbook.service.PlaybookService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/playbook", "/api/playbook"})
public class PlaybookController {
    private final PlaybookService playbookService;
    private final PlaybookAnalysisService analysisService;
    private final FeatureUsageService featureUsageService;
    private final FeatureFlagService featureFlagService;

    public PlaybookController(PlaybookService playbookService, PlaybookAnalysisService analysisService, FeatureUsageService featureUsageService, FeatureFlagService featureFlagService) {
        this.playbookService = playbookService;
        this.analysisService = analysisService;
        this.featureUsageService = featureUsageService;
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public List<PlaybookSetupSummaryResponse> list(@AuthenticationPrincipal User user,
                                                   @RequestParam(required = false) Long accountId,
                                                   @RequestParam(required = false) PlaybookStatus status,
                                                   @RequestParam(required = false) String search,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        featureUsageService.track(user, FeatureKey.PLAYBOOK);
        return playbookService.list(user, accountId, status, search, page, size);
    }

    @GetMapping("/overview")
    public PlaybookOverviewResponse overview(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        featureUsageService.track(user, FeatureKey.PLAYBOOK);
        return playbookService.overview(user, accountId);
    }

    @PostMapping
    public PlaybookSetupDetailResponse create(@AuthenticationPrincipal User user, @RequestBody PlaybookSetupRequest request) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        featureUsageService.track(user, FeatureKey.PLAYBOOK);
        return playbookService.create(user, request);
    }

    @GetMapping("/{setupId}")
    public PlaybookSetupDetailResponse get(@AuthenticationPrincipal User user, @PathVariable Long setupId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        featureUsageService.track(user, FeatureKey.PLAYBOOK);
        return playbookService.get(user, setupId);
    }

    @PutMapping("/{setupId}")
    public PlaybookSetupDetailResponse update(@AuthenticationPrincipal User user, @PathVariable Long setupId, @RequestBody PlaybookSetupRequest request) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        featureUsageService.track(user, FeatureKey.PLAYBOOK);
        return playbookService.update(user, setupId, request);
    }

    @DeleteMapping("/{setupId}")
    public PlaybookSetupDetailResponse archive(@AuthenticationPrincipal User user, @PathVariable Long setupId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.archive(user, setupId);
    }

    @PostMapping("/{setupId}/restore")
    public PlaybookSetupDetailResponse restore(@AuthenticationPrincipal User user, @PathVariable Long setupId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.restore(user, setupId);
    }

    @PostMapping("/{setupId}/duplicate")
    public PlaybookSetupDetailResponse duplicate(@AuthenticationPrincipal User user, @PathVariable Long setupId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.duplicate(user, setupId);
    }

    @PostMapping("/{setupId}/trades/{tradeId}")
    public PlaybookTradeAssignmentResponse link(@AuthenticationPrincipal User user, @PathVariable Long setupId, @PathVariable Long tradeId, @RequestBody(required = false) PlaybookTradeAssignmentRequest request) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.linkTrade(user, setupId, tradeId, request);
    }

    @DeleteMapping("/{setupId}/trades/{tradeId}")
    public void unlink(@AuthenticationPrincipal User user, @PathVariable Long setupId, @PathVariable Long tradeId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        playbookService.unlinkTrade(user, setupId, tradeId);
    }

    @PostMapping("/{setupId}/trades/bulk-link")
    public List<PlaybookTradeAssignmentResponse> bulkLink(@AuthenticationPrincipal User user, @PathVariable Long setupId, @RequestBody BulkLinkTradesRequest request) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.bulkLink(user, setupId, request);
    }

    @GetMapping("/{setupId}/trades")
    public List<PlaybookTradeAssignmentResponse> trades(@AuthenticationPrincipal User user, @PathVariable Long setupId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.linkedTrades(user, setupId);
    }

    @PatchMapping("/assignments/{assignmentId}")
    public PlaybookTradeAssignmentResponse updateAssignment(@AuthenticationPrincipal User user, @PathVariable Long assignmentId, @RequestBody PlaybookTradeAssignmentRequest request) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.updateAssignment(user, assignmentId, request);
    }

    @GetMapping("/{setupId}/analysis")
    public PlaybookAnalysisResponse analysis(@AuthenticationPrincipal User user, @PathVariable Long setupId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        featureUsageService.track(user, FeatureKey.PLAYBOOK);
        return analysisService.getAnalysis(user, setupId);
    }
}
