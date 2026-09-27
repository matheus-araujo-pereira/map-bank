package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para formalização e contratação de empréstimo bancário.
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Payload para contratação formal de empréstimo")
public record LoanContractRequest(
        @Schema(description = "Número da conta bancária beneficiária", example = "10001-9")
        @NotBlank(message = "A conta bancária é obrigatória")
        String accountNumber,

        @Schema(description = "Valor principal a ser contratado", example = "10000.00")
        @NotNull(message = "O valor solicitado é obrigatório")
        @DecimalMin(value = "500.00", message = "O valor mínimo de contratação é R$ 500,00")
        BigDecimal requestedAmount,

        @Schema(description = "Quantidade de parcelas acordadas", example = "24")
        @NotNull(message = "A quantidade de parcelas é obrigatória")
        @Min(value = 1, message = "Mínimo de 1 parcela")
        @Max(value = 72, message = "Máximo de 72 parcelas")
        Integer installments
) {}
