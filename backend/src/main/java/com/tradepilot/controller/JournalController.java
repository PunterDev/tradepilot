package com.tradepilot.controller;

import com.tradepilot.dto.journal.JournalRequest;
import com.tradepilot.dto.journal.JournalResponse;
import com.tradepilot.entity.User;
import com.tradepilot.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @PostMapping
    public JournalResponse save(
            @RequestBody JournalRequest request,
            @AuthenticationPrincipal User user
    ) {
        return journalService.save(user, request);
    }

    @GetMapping
    public List<JournalResponse> getAll(@AuthenticationPrincipal User user) {
        return journalService.getAll(user);
    }

    @GetMapping("/by-date")
    public JournalResponse getByDate(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return journalService.getByDate(user, accountId, date);
    }
}