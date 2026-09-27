package com.mapbank.dto.request;

import com.mapbank.domain.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para abertura de uma nova conta bancária vinculada a um cliente.
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Payload para abertura de conta bancária")
public record CreateAccountRequest(
        @Schema(description = "ID do cliente titular", example = "1")
        @NotNull(message = "O ID do cliente titular é obrigatório")
        Long clientId,

        @Schema(description = "Tipo de conta bancária", example = "CORRENTE")
        @NotNull(message = "O tipo de conta é obrigatório")
        AccountType accountType,

        @Schema(description = "Depósito inicial na abertura (opcional)", example = "500.00")
        @DecimalMin(value = "0.00", message = "O depósito inicial não pode ser negativo")
        BigDecimal initialDeposit,

        @Schema(description = "Limite de cheque especial concedido", example = "1000.00")
        @DecimalMin(value = "0.00", message = "O limite de cheque especial não pode ser negativo")
        BigDecimal overdraftLimit
) {}
