package com.tradepilot.dto.auth;

public class UserMeResponse {
    private Long id;
    private String name;
    private String email;
    private String plan;
    private String role;

    public UserMeResponse() {
    }

    public UserMeResponse(Long id, String name, String email, String plan, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.plan = plan;
        this.role = role;
    }

    public static UserMeResponse of(Long id, String name, String email, String plan, String role) {
        return new UserMeResponse(id, name, email, plan, role);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
