package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para ajuste do limite de cheque especial da conta bancária.
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Payload para ajuste do limite de cheque especial")
public record UpdateAccountLimitRequest(
        @Schema(description = "Novo valor do limite de cheque especial", example = "5000.00")
        @NotNull(message = "O limite é obrigatório")
        @DecimalMin(value = "0.00", message = "O limite não pode ser negativo")
        BigDecimal overdraftLimit
) {}
