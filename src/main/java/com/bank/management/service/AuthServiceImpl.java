package com.bank.management.service;

import com.bank.management.api.error.NotFoundException;
import com.bank.management.config.JwtProperties;
import com.bank.management.dto.request.LoginRequest;
import com.bank.management.dto.response.TokenResponse;
import com.bank.management.entity.User;
import com.bank.management.repository.UserRepository;
import com.bank.management.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        String token = jwtService.generateToken(user);
        long seconds = jwtProperties.accessTokenMinutes() * 60L;
        return TokenResponse.of(token, seconds);
    }
}
