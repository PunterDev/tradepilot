package com.tradepilot.controller;

import com.tradepilot.dto.ai.*;
import com.tradepilot.entity.User;
import com.tradepilot.service.AiReflectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiReflectionController {
    private final AiReflectionService aiReflectionService;

    @PostMapping("/weekly-review")
    public WeeklyReflectionResponse weeklyReview(@Valid @RequestBody WeeklyReflectionRequest request, @AuthenticationPrincipal User user) {
        return aiReflectionService.generateWeeklyReflection(user, request);
    }
}
