package com.bank.management.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create card request body.
 */
public record CardCreateRequest(
        @NotBlank
        @Pattern(regexp = "\\d{13,19}", message = "PAN must be 13-19 digits")
        String pan,
        @NotNull
        @Future
        LocalDate expiryDate,
        @PositiveOrZero
        BigDecimal initialBalance,
        Long ownerUserId
) {
    public CardCreateRequest {
        initialBalance = initialBalance == null ? BigDecimal.ZERO : initialBalance;
    }
}
