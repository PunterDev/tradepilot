package com.tradepilot.dto.auth;

public class AuthResponse {
    private String token;
    private Long userId;
    private String name;
    private String email;
    private String plan;
    private String role;

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

    public static AuthResponse of(String token, Long userId, String name, String email, String plan, String role) {
        return new AuthResponse(token, userId, name, email, plan, role);
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
}
