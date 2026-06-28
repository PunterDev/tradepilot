package com.tradepilot.coach.controller;

import com.tradepilot.coach.dto.CoachGenerateRequest;
import com.tradepilot.coach.dto.CoachSnapshotDto;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.service.AiCoachService;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/coach")
public class AiCoachController {
    private final AiCoachService aiCoachService;
    private final FeatureUsageService featureUsageService;

    @GetMapping("/daily")
    public CoachSnapshotDto getDailyCoach(@AuthenticationPrincipal User user, @RequestParam Long accountId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        CoachSnapshotDto response = aiCoachService.getDailyCoach(user, accountId, date);
        featureUsageService.track(user, FeatureKey.DAILY_COACH);
        return response;
    }

    @GetMapping("/weekly")
    public CoachSnapshotDto getWeeklyCoach(@AuthenticationPrincipal User user, @RequestParam Long accountId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        CoachSnapshotDto response = aiCoachService.getWeeklyCoach(user, accountId, weekStart);
        featureUsageService.track(user, FeatureKey.WEEKLY_COACH);
        return response;
    }

    @PostMapping("/daily/generate")
    public CoachSnapshotDto generateDailyCoach(@AuthenticationPrincipal User user, @RequestBody CoachGenerateRequest request) {
        CoachSnapshotDto response = aiCoachService.generateDailyCoach(user, request);
        featureUsageService.track(user, FeatureKey.DAILY_COACH);
        return response;
    }

    @PostMapping("/weekly/generate")
    public CoachSnapshotDto generateWeeklyCoach(@AuthenticationPrincipal User user, @RequestBody CoachGenerateRequest request) {
        CoachSnapshotDto response = aiCoachService.generateWeeklyCoach(user, request);
        featureUsageService.track(user, FeatureKey.WEEKLY_COACH);
        return response;
    }

    @GetMapping("/snapshots")
    public List<CoachSnapshotDto> getSnapshots(@AuthenticationPrincipal User user, @RequestParam Long accountId, @RequestParam CoachSnapshotType type) {
        return aiCoachService.getSnapshots(user, accountId, type);
    }

    @java.lang.SuppressWarnings("all")
    public AiCoachController(final AiCoachService aiCoachService, final FeatureUsageService featureUsageService) {
        this.aiCoachService = aiCoachService;
        this.featureUsageService = featureUsageService;
    }
}
