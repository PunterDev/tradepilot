package com.tradepilot.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Email
    @Size(max = 150)
    private String email;
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
}
