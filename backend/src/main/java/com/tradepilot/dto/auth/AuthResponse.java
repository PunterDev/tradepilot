package com.tradepilot.dto.auth;

public class AuthResponse {
    private String token;
    private Long userId;
    private String name;
    private String email;
    private String plan;
    private String role;
    private Integer monthlyAiCredits;
    private Integer monthlyAiCreditsUsed;
    private Integer monthlyAiCreditsRemaining;
    private Integer purchasedAiCreditsBalance;
    private Integer totalAiCreditsAvailable;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long userId, String name, String email, String plan, String role) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.plan = plan;
        this.role = role;
    }

    public AuthResponse(String token, Long userId, String name, String email, String plan, String role,
                        Integer monthlyAiCredits, Integer monthlyAiCreditsUsed, Integer monthlyAiCreditsRemaining,
                        Integer purchasedAiCreditsBalance, Integer totalAiCreditsAvailable) {
        this(token, userId, name, email, plan, role);
        this.monthlyAiCredits = monthlyAiCredits;
        this.monthlyAiCreditsUsed = monthlyAiCreditsUsed;
        this.monthlyAiCreditsRemaining = monthlyAiCreditsRemaining;
        this.purchasedAiCreditsBalance = purchasedAiCreditsBalance;
        this.totalAiCreditsAvailable = totalAiCreditsAvailable;
    }

    public static AuthResponse of(String token, Long userId, String name, String email, String plan, String role) {
        return new AuthResponse(token, userId, name, email, plan, role);
    }

    public static AuthResponse of(String token, Long userId, String name, String email, String plan, String role,
                                  Integer monthlyAiCredits, Integer monthlyAiCreditsUsed, Integer monthlyAiCreditsRemaining,
                                  Integer purchasedAiCreditsBalance, Integer totalAiCreditsAvailable) {
        return new AuthResponse(token, userId, name, email, plan, role, monthlyAiCredits, monthlyAiCreditsUsed,
                monthlyAiCreditsRemaining, purchasedAiCreditsBalance, totalAiCreditsAvailable);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getMonthlyAiCredits() {
        return monthlyAiCredits;
    }

    public void setMonthlyAiCredits(Integer monthlyAiCredits) {
        this.monthlyAiCredits = monthlyAiCredits;
    }

    public Integer getMonthlyAiCreditsUsed() {
        return monthlyAiCreditsUsed;
    }

    public void setMonthlyAiCreditsUsed(Integer monthlyAiCreditsUsed) {
        this.monthlyAiCreditsUsed = monthlyAiCreditsUsed;
    }

    public Integer getMonthlyAiCreditsRemaining() {
        return monthlyAiCreditsRemaining;
    }

    public void setMonthlyAiCreditsRemaining(Integer monthlyAiCreditsRemaining) {
        this.monthlyAiCreditsRemaining = monthlyAiCreditsRemaining;
    }

    public Integer getPurchasedAiCreditsBalance() {
        return purchasedAiCreditsBalance;
    }

    public void setPurchasedAiCreditsBalance(Integer purchasedAiCreditsBalance) {
        this.purchasedAiCreditsBalance = purchasedAiCreditsBalance;
    }

    public Integer getTotalAiCreditsAvailable() {
        return totalAiCreditsAvailable;
    }

    public void setTotalAiCreditsAvailable(Integer totalAiCreditsAvailable) {
        this.totalAiCreditsAvailable = totalAiCreditsAvailable;
    }
}
