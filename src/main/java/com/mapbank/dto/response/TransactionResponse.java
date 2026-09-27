package com.mapbank.dto.response;

import com.mapbank.domain.enums.TransactionStatus;
import com.mapbank.domain.enums.TransactionType;
import com.mapbank.domain.model.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Record (Java 21) contendo o comprovante e extrato detalhado de uma transação financeira.
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Comprovante e dados da transação bancária")
public record TransactionResponse(
        @Schema(description = "Identificador único da transação", example = "1")
        Long id,

        @Schema(description = "Código de autorização e idempotência único", example = "TX-9b2f3a1e-84cb")
        String transactionCode,

        @Schema(description = "Conta pagadora (origem)", example = "10001-9")
        String sourceAccountNumber,

        @Schema(description = "Conta recebedora (destino)", example = "20002-8")
        String targetAccountNumber,

        @Schema(description = "Valor transacionado", example = "350.00")
        BigDecimal amount,

        @Schema(description = "Tipo da operação realizada", example = "PIX")
        TransactionType transactionType,

        @Schema(description = "Status de liquidação", example = "COMPLETED")
        TransactionStatus status,

        @Schema(description = "Descrição ou finalidade informada", example = "Consultoria técnica NTT DATA")
        String description,

        @Schema(description = "Motivo de rejeição ou falha (quando aplicável)")
        String failureReason,

        @Schema(description = "Timestamp do processamento da transação")
        Instant createdAt
) {
    public static TransactionResponse fromEntity(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getTransactionCode(),
                tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : null,
                tx.getTargetAccount() != null ? tx.getTargetAccount().getAccountNumber() : null,
                tx.getAmount(),
                tx.getTransactionType(),
                tx.getStatus(),
                tx.getDescription(),
                tx.getFailureReason(),
                tx.getCreatedAt()
        );
    }
}
