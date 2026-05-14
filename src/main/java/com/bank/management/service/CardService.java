package com.bank.management.service;

import com.bank.management.dto.request.CardCreateRequest;
import com.bank.management.dto.response.CardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CardService {

    CardResponse create(CardCreateRequest request, String actorUsername);

    CardResponse getById(Long id, String actorUsername);

    Page<CardResponse> list(Pageable pageable, String actorUsername);
}
