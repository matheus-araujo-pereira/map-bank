package com.mapbank.dto.request;

import com.mapbank.domain.enums.PixKeyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Record (Java 21) para cadastro de nova chave de endereçamento PIX vinculada a uma conta.
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Payload para registro de chave PIX")
public record CreatePixKeyRequest(
        @Schema(description = "Tipo da chave PIX", example = "EMAIL")
        @NotNull(message = "O tipo da chave PIX é obrigatório")
        PixKeyType keyType,

        @Schema(description = "Valor da chave PIX (CPF, CNPJ, Email, Telefone ou UUID se RANDOM)", example = "matheus@mapbank.com")
        @NotBlank(message = "O valor da chave PIX é obrigatório")
        String keyValue
) {}
