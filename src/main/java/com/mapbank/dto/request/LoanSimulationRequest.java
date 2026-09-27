package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para solicitação de simulação de crédito e financiamento bancário.
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Payload para simulação de proposta de empréstimo")
public record LoanSimulationRequest(
        @Schema(description = "Valor principal solicitado", example = "10000.00")
        @NotNull(message = "O valor solicitado é obrigatório")
        @DecimalMin(value = "500.00", message = "O valor mínimo de empréstimo é de R$ 500,00")
        BigDecimal requestedAmount,

        @Schema(description = "Quantidade de parcelas mensais desejada", example = "24")
        @NotNull(message = "A quantidade de parcelas é obrigatória")
        @Min(value = 1, message = "O número mínimo de parcelas é 1")
        @Max(value = 72, message = "O número máximo de parcelas é 72")
        Integer installments
) {}
