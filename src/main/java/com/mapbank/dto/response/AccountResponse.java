package com.mapbank.dto.response;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.enums.AccountType;
import com.mapbank.domain.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Record (Java 21) contendo as informações financeiras e cadastrais da conta bancária.
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Dados detalhados e saldo da conta bancária")
public record AccountResponse(
        @Schema(description = "Identificador único da conta", example = "1")
        Long id,

        @Schema(description = "Número da conta com dígito", example = "10001-9")
        String accountNumber,

        @Schema(description = "Número da agência", example = "0001")
        String agency,

        @Schema(description = "Tipo de conta bancária", example = "CORRENTE")
        AccountType accountType,

        @Schema(description = "Saldo contábil real disponível", example = "15000.00")
        BigDecimal balance,

        @Schema(description = "Limite de cheque especial contratado", example = "3000.00")
        BigDecimal overdraftLimit,

        @Schema(description = "Total de recursos disponíveis (Saldo + Cheque Especial)", example = "18000.00")
        BigDecimal totalAvailableFunds,

        @Schema(description = "Situação da conta", example = "ACTIVE")
        AccountStatus status,

        @Schema(description = "ID do cliente titular", example = "1")
        Long clientId,

        @Schema(description = "Nome do cliente titular", example = "Matheus Araújo Pereira")
        String clientName,

        @Schema(description = "Data de abertura da conta")
        Instant createdAt
) {
    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAgency(),
                account.getAccountType(),
                account.getBalance(),
                account.getOverdraftLimit(),
                account.getTotalAvailableFunds(),
                account.getStatus(),
                account.getClient() != null ? account.getClient().getId() : null,
                account.getClient() != null ? account.getClient().getName() : null,
                account.getCreatedAt()
        );
    }
}
