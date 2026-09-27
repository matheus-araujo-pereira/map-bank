package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para transferência entre contas internas do MAP-Bank.
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Payload para transferência bancária entre contas")
public record InternalTransferRequest(
        @Schema(description = "Número da conta bancária de origem", example = "10001-9")
        @NotBlank(message = "A conta de origem é obrigatória")
        String sourceAccountNumber,

        @Schema(description = "Número da conta bancária de destino", example = "20002-8")
        @NotBlank(message = "A conta de destino é obrigatória")
        String targetAccountNumber,

        @Schema(description = "Valor a ser transferido", example = "1500.00")
        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor da transferência deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Descrição ou finalidade da transferência", example = "Pagamento de serviços prestados")
        String description
) {}
