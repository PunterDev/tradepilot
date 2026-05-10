package com.tradepilot.service;

import com.tradepilot.dto.auth.*;
import com.tradepilot.entity.User;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("Email is already registered");
        User user = User.builder().name(request.getName()).email(email).passwordHash(passwordEncoder.encode(request.getPassword())).build();
        User saved = userRepository.save(user);
        return AuthResponse.builder().token(jwtService.generateToken(saved)).userId(saved.getId()).name(saved.getName()).email(saved.getEmail()).build();
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return AuthResponse.builder().token(jwtService.generateToken(user)).userId(user.getId()).name(user.getName()).email(user.getEmail()).build();
    }

    public UserMeResponse me(User user) {
        return UserMeResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail()).build();
    }
}
