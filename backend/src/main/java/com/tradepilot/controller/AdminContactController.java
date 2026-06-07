package com.tradepilot.controller;

import com.tradepilot.dto.admin.AdminContactMessageResponse;
import com.tradepilot.dto.admin.AdminContactMessageNotesRequest;
import com.tradepilot.dto.admin.AdminContactMessageReplyRequest;
import com.tradepilot.dto.admin.AdminContactMessageStatusRequest;
import com.tradepilot.dto.admin.AdminContactMessageSummaryResponse;
import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.enums.ContactMessageStatus;
import com.tradepilot.service.AdminContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/contact-messages")
@RequiredArgsConstructor
public class AdminContactController {
    private final AdminContactMessageService adminContactMessageService;

    @GetMapping
    public PagedResponse<AdminContactMessageResponse> list(
            @RequestParam(required = false) ContactMessageStatus status,
            @RequestParam(required = false) Boolean emailFailed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return adminContactMessageService.list(status, emailFailed, page, size, sortBy, sortDir);
    }

    @GetMapping("/summary")
    public AdminContactMessageSummaryResponse summary() {
        return adminContactMessageService.summary();
    }

    @GetMapping("/{id}")
    public AdminContactMessageResponse get(@PathVariable Long id) {
        return adminContactMessageService.get(id);
    }

    @PatchMapping("/{id}/status")
    public AdminContactMessageResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminContactMessageStatusRequest request
    ) {
        return adminContactMessageService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/notes")
    public AdminContactMessageResponse updateNotes(
            @PathVariable Long id,
            @Valid @RequestBody AdminContactMessageNotesRequest request
    ) {
        return adminContactMessageService.updateNotes(id, request.adminNotes());
    }

    @PostMapping("/{id}/reply")
    public AdminContactMessageResponse reply(
            @PathVariable Long id,
            @Valid @RequestBody AdminContactMessageReplyRequest request
    ) {
        return adminContactMessageService.reply(id, request);
    }
}
