package com.mapbank.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Record (Java 21) contendo a simulação calculada de proposta de crédito (Tabela Price).
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Resultado do cálculo e projeção de empréstimo")
public record LoanSimulationResponse(
        @Schema(description = "Valor solicitado", example = "10000.00")
        BigDecimal requestedAmount,

        @Schema(description = "Taxa de juros mensal aplicada (ex: 0.0250 = 2.50% a.m.)", example = "0.0250")
        BigDecimal interestRateMonthly,

        @Schema(description = "Taxa percentual expressa ao mês", example = "2.50%")
        String interestRateFormatted,

        @Schema(description = "Quantidade de parcelas mensais", example = "24")
        Integer installments,

        @Schema(description = "Valor fixo de cada parcela", example = "560.12")
        BigDecimal installmentAmount,

        @Schema(description = "Custo Total Efetivo da operação (Total a pagar)", example = "13442.88")
        BigDecimal totalAmount,

        @Schema(description = "Total de juros incorridos na operação", example = "3442.88")
        BigDecimal totalInterest
) {}
