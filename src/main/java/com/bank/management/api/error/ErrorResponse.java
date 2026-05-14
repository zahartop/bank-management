package com.bank.management.api.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Standard API error payload.
 */
@Schema(description = "Standard error envelope")
public record ErrorResponse(Instant timestamp, String message, String code) {
}
