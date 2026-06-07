package com.tradepilot.controller;

import com.tradepilot.dto.admin.AdminPlanChangeRequest;
import com.tradepilot.dto.admin.AdminUserDetailResponse;
import com.tradepilot.dto.admin.AdminUserResponse;
import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.entity.User;
import com.tradepilot.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public PagedResponse<AdminUserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return adminUserService.listUsers(page, size, sortBy, sortDir);
    }

    @GetMapping("/search")
    public PagedResponse<AdminUserResponse> searchUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return adminUserService.searchUsers(query, page, size, sortBy, sortDir);
    }

    @GetMapping("/{userId}")
    public AdminUserDetailResponse getUser(@PathVariable Long userId) {
        return adminUserService.getUser(userId);
    }

    @PostMapping("/{userId}/upgrade-pro")
    public AdminUserResponse upgradePro(
            @AuthenticationPrincipal User admin,
            @PathVariable Long userId,
            @Valid @RequestBody AdminPlanChangeRequest request
    ) {
        return adminUserService.upgradeToPro(admin, userId, request);
    }

    @PostMapping("/{userId}/downgrade-free")
    public AdminUserResponse downgradeFree(
            @AuthenticationPrincipal User admin,
            @PathVariable Long userId,
            @Valid @RequestBody AdminPlanChangeRequest request
    ) {
        return adminUserService.downgradeToFree(admin, userId, request);
    }

    @PostMapping("/{userId}/make-admin")
    public AdminUserResponse makeAdmin(
            @AuthenticationPrincipal User admin,
            @PathVariable Long userId,
            @Valid @RequestBody AdminPlanChangeRequest request
    ) {
        return adminUserService.makeAdmin(admin, userId, request);
    }

    @PostMapping("/{userId}/remove-admin")
    public AdminUserResponse removeAdmin(
            @AuthenticationPrincipal User admin,
            @PathVariable Long userId,
            @Valid @RequestBody AdminPlanChangeRequest request
    ) {
        return adminUserService.removeAdmin(admin, userId, request);
    }
}
