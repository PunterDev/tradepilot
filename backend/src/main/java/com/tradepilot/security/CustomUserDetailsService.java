package com.tradepilot.security;

import com.tradepilot.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @java.lang.SuppressWarnings("all")
    public CustomUserDetailsService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
