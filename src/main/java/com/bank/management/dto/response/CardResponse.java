package com.bank.management.dto.response;

import com.bank.management.entity.CardStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Card representation for API (PAN is always masked).
 */
public record CardResponse(
        Long id,
        Long userId,
        String maskedPan,
        LocalDate expiryDate,
        CardStatus status,
        BigDecimal balance
) {
}
