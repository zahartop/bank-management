package com.bank.management.service;

import com.bank.management.dto.request.LoginRequest;
import com.bank.management.dto.response.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);
}
