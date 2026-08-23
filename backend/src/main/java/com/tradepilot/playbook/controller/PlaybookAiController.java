package com.tradepilot.playbook.controller;

import com.tradepilot.billing.Feature;
import com.tradepilot.config.FeatureFlagService;
import com.tradepilot.entity.User;
import com.tradepilot.playbook.dto.PlaybookAnalysisResponse;
import com.tradepilot.playbook.dto.PlaybookDraftRequest;
import com.tradepilot.playbook.dto.PlaybookDraftResponse;
import com.tradepilot.playbook.service.PlaybookAnalysisService;
import com.tradepilot.playbook.service.PlaybookService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/ai/playbook", "/api/ai/playbook"})
public class PlaybookAiController {
    private final PlaybookService playbookService;
    private final PlaybookAnalysisService analysisService;
    private final FeatureFlagService featureFlagService;

    public PlaybookAiController(PlaybookService playbookService, PlaybookAnalysisService analysisService, FeatureFlagService featureFlagService) {
        this.playbookService = playbookService;
        this.analysisService = analysisService;
        this.featureFlagService = featureFlagService;
    }

    @PostMapping("/draft-from-description")
    public PlaybookDraftResponse draftFromDescription(@AuthenticationPrincipal User user, @RequestBody PlaybookDraftRequest request) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.draftFromDescription(user, request);
    }

    @PostMapping("/draft-from-trades")
    public PlaybookDraftResponse draftFromTrades(@AuthenticationPrincipal User user, @RequestBody PlaybookDraftRequest request) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return playbookService.draftFromTrades(user, request);
    }

    @PostMapping("/{setupId}/regenerate")
    public PlaybookAnalysisResponse regenerate(@AuthenticationPrincipal User user, @PathVariable Long setupId) {
        featureFlagService.requireEnabled(Feature.PLAYBOOK);
        return analysisService.regenerate(user, setupId);
    }
}
