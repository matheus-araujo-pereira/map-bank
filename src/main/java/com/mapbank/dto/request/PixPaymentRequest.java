package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Record (Java 21) para transferência instantânea via Arranjo PIX.
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Payload para realização de transferência instantânea via PIX")
public record PixPaymentRequest(
        @Schema(description = "Número da conta pagadora (origem)", example = "10001-9")
        @NotBlank(message = "A conta de origem é obrigatória")
        String sourceAccountNumber,

        @Schema(description = "Chave PIX do destinatário (CPF, CNPJ, Email, Telefone ou EVP)", example = "contato@nttdata.com")
        @NotBlank(message = "A chave PIX de destino é obrigatória")
        String targetPixKey,

        @Schema(description = "Valor do pagamento PIX", example = "350.00")
        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor do PIX deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Mensagem ou descrição opcional enviada ao destinatário", example = "Consultoria técnica NTT DATA")
        String description
) {}
