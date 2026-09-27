package com.mapbank.dto.response;

import com.mapbank.domain.enums.LoanStatus;
import com.mapbank.domain.model.Loan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Record (Java 21) contendo os dados do contrato de empréstimo ativo no MAP-Bank.
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Dados do contrato de empréstimo contratado")
public record LoanResponse(
        @Schema(description = "Identificador único do contrato", example = "1")
        Long id,

        @Schema(description = "Código único do contrato de crédito", example = "LN-8c3e21ab")
        String loanCode,

        @Schema(description = "Conta bancária vinculada ao contrato", example = "10001-9")
        String accountNumber,

        @Schema(description = "Valor principal contratado", example = "10000.00")
        BigDecimal requestedAmount,

        @Schema(description = "Taxa de juros mensal contratada", example = "0.0250")
        BigDecimal interestRateMonthly,

        @Schema(description = "Número total de parcelas", example = "24")
        Integer installments,

        @Schema(description = "Valor fixo da parcela mensal", example = "560.12")
        BigDecimal installmentAmount,

        @Schema(description = "Valor total do contrato", example = "13442.88")
        BigDecimal totalAmount,

        @Schema(description = "Saldo devedor remanescente", example = "12882.76")
        BigDecimal remainingBalance,

        @Schema(description = "Situação do contrato", example = "APPROVED")
        LoanStatus status,

        @Schema(description = "Data de contratação")
        Instant createdAt
) {
    public static LoanResponse fromEntity(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getLoanCode(),
                loan.getAccount() != null ? loan.getAccount().getAccountNumber() : null,
                loan.getRequestedAmount(),
                loan.getInterestRateMonthly(),
                loan.getInstallments(),
                loan.getInstallmentAmount(),
                loan.getTotalAmount(),
                loan.getRemainingBalance(),
                loan.getStatus(),
                loan.getCreatedAt()
        );
    }
}
