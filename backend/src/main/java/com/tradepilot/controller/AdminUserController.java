package com.tradepilot.controller;

import com.tradepilot.dto.admin.AdminPlanChangeRequest;
import com.tradepilot.dto.admin.AdminUserDetailResponse;
import com.tradepilot.dto.admin.AdminUserResponse;
import com.tradepilot.dto.admin.UserFeatureEntitlementRequest;
import com.tradepilot.dto.admin.UserFeatureEntitlementResponse;
import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.entity.User;
import com.tradepilot.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public PagedResponse<AdminUserResponse> listUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size, @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "desc") String sortDir) {
        return adminUserService.listUsers(page, size, sortBy, sortDir);
    }

    @GetMapping("/search")
    public PagedResponse<AdminUserResponse> searchUsers(@RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size, @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "desc") String sortDir) {
        return adminUserService.searchUsers(query, page, size, sortBy, sortDir);
    }

    @GetMapping("/{userId}")
    public AdminUserDetailResponse getUser(@PathVariable Long userId) {
        return adminUserService.getUser(userId);
    }

    @PostMapping("/{userId}/upgrade-pro")
    public AdminUserResponse upgradePro(@AuthenticationPrincipal User admin, @PathVariable Long userId, @Valid @RequestBody AdminPlanChangeRequest request) {
        return adminUserService.upgradeToPro(admin, userId, request);
    }

    @PostMapping("/{userId}/downgrade-free")
    public AdminUserResponse downgradeFree(@AuthenticationPrincipal User admin, @PathVariable Long userId, @Valid @RequestBody AdminPlanChangeRequest request) {
        return adminUserService.downgradeToFree(admin, userId, request);
    }

    @PostMapping("/{userId}/make-admin")
    public AdminUserResponse makeAdmin(@AuthenticationPrincipal User admin, @PathVariable Long userId, @Valid @RequestBody AdminPlanChangeRequest request) {
        return adminUserService.makeAdmin(admin, userId, request);
    }

    @PostMapping("/{userId}/remove-admin")
    public AdminUserResponse removeAdmin(@AuthenticationPrincipal User admin, @PathVariable Long userId, @Valid @RequestBody AdminPlanChangeRequest request) {
        return adminUserService.removeAdmin(admin, userId, request);
    }

    @GetMapping("/{userId}/feature-entitlements")
    public List<UserFeatureEntitlementResponse> featureEntitlements(@PathVariable Long userId) {
        return adminUserService.featureEntitlements(userId);
    }

    @PutMapping("/{userId}/feature-entitlements")
    public UserFeatureEntitlementResponse setFeatureEntitlement(@AuthenticationPrincipal User admin, @PathVariable Long userId, @RequestBody UserFeatureEntitlementRequest request) {
        return adminUserService.setFeatureEntitlement(admin, userId, request);
    }

    @java.lang.SuppressWarnings("all")
    public AdminUserController(final AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }
}
