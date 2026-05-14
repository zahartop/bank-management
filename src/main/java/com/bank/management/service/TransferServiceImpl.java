package com.bank.management.service;

import com.bank.management.api.error.BankException;
import com.bank.management.api.error.ForbiddenException;
import com.bank.management.api.error.NotFoundException;
import com.bank.management.dto.request.TransferRequest;
import com.bank.management.entity.Card;
import com.bank.management.entity.CardStatus;
import com.bank.management.entity.User;
import com.bank.management.repository.CardRepository;
import com.bank.management.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(TransferRequest request, String actorUsername) {
        User user = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        long low = Math.min(request.fromCardId(), request.toCardId());
        long high = Math.max(request.fromCardId(), request.toCardId());

        Card lower = cardRepository.findByIdForUpdate(low)
                .orElseThrow(() -> new NotFoundException("CARD_NOT_FOUND", "Card not found"));
        Card upper = cardRepository.findByIdForUpdate(high)
                .orElseThrow(() -> new NotFoundException("CARD_NOT_FOUND", "Card not found"));

        Card fromCard = lower.getId().equals(request.fromCardId()) ? lower : upper;
        Card toCard = fromCard == lower ? upper : lower;

        validateTransfer(user, fromCard, toCard, request.amount());

        fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
        toCard.setBalance(toCard.getBalance().add(request.amount()));
    }

    private void validateTransfer(User user, Card from, Card to, BigDecimal amount) {
        if (!from.getUser().getId().equals(user.getId()) || !to.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("TRANSFER_FORBIDDEN", "Both cards must belong to you");
        }
        if (from.getId().equals(to.getId())) {
            throw new BankException("TRANSFER_SAME_CARD", "Cannot transfer to the same card");
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (from.getExpiryDate().isBefore(today) || to.getExpiryDate().isBefore(today)) {
            throw new BankException("CARD_EXPIRED", "One of the cards is expired");
        }
        if (from.getStatus() != CardStatus.ACTIVE || to.getStatus() != CardStatus.ACTIVE) {
            throw new BankException("CARD_INACTIVE", "Both cards must be ACTIVE");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new BankException("INSUFFICIENT_FUNDS", "Insufficient balance on source card");
        }
    }
}
