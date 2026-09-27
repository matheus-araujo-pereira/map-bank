package com.mapbank.dto.response;

import com.mapbank.domain.enums.PixKeyType;
import com.mapbank.domain.model.PixKey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Record (Java 21) contendo os dados da chave PIX registrada.
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Dados da chave PIX cadastrada")
public record PixKeyResponse(
        @Schema(description = "ID da chave PIX", example = "1")
        Long id,

        @Schema(description = "Valor da chave PIX", example = "matheus@mapbank.com")
        String keyValue,

        @Schema(description = "Tipo da chave PIX", example = "EMAIL")
        PixKeyType keyType,

        @Schema(description = "Número da conta associada", example = "10001-9")
        String accountNumber,

        @Schema(description = "Status da chave", example = "ACTIVE")
        String status,

        @Schema(description = "Data de registro da chave")
        Instant createdAt
) {
    public static PixKeyResponse fromEntity(PixKey pixKey) {
        return new PixKeyResponse(
                pixKey.getId(),
                pixKey.getKeyValue(),
                pixKey.getKeyType(),
                pixKey.getAccount() != null ? pixKey.getAccount().getAccountNumber() : null,
                pixKey.getStatus(),
                pixKey.getCreatedAt()
        );
    }
}
