package com.bank.management.service;

import com.bank.management.dto.request.TransferRequest;

public interface TransferService {

    void transfer(TransferRequest request, String actorUsername);
}
