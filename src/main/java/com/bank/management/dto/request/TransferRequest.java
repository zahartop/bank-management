package com.bank.management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Transfer money between own cards.
 */
public record TransferRequest(
        @NotNull @Positive Long fromCardId,
        @NotNull @Positive Long toCardId,
        @NotNull @Positive BigDecimal amount
) {
}
