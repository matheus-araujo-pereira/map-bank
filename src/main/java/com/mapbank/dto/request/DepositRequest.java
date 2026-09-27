package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para realização de depósito em conta bancária.
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Payload para realização de depósito financeiro em conta")
public record DepositRequest(
        @Schema(description = "Valor a ser creditado na conta", example = "250.00")
        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor do depósito deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Descrição ou observação opcional do depósito", example = "Depósito em dinheiro via terminal")
        String description
) {}
