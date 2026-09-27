package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para realização de saque em conta bancária.
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Payload para realização de saque em espécie da conta")
public record WithdrawRequest(
        @Schema(description = "Valor a ser debitado da conta", example = "100.00")
        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor do saque deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Descrição opcional do saque", example = "Saque terminal 24h")
        String description
) {}
