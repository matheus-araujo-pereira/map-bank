package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para amortização ou quitação de parcela de empréstimo bancário.
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Payload para amortização / pagamento de parcela de empréstimo")
public record LoanPaymentRequest(
        @Schema(description = "Valor a ser amortizado", example = "538.50")
        @NotNull(message = "O valor do pagamento é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal amount
) {}
