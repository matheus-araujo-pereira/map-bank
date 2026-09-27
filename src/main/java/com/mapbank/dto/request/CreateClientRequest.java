package com.mapbank.dto.request;

import com.mapbank.domain.enums.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Record (Java 21) para requisição de cadastro de novo cliente titular no MAP-Bank.
 *
 * <p>Demonstra imutabilidade de dados, validações de Bean Validation Jakarta e anotações OpenAPI.</p>
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Payload para abertura de cadastro de cliente titular (PF/PJ)")
public record CreateClientRequest(
        @Schema(description = "Nome completo ou Razão Social", example = "Matheus Araújo Pereira")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String name,

        @Schema(description = "Documento identificador único (CPF com 11 dígitos ou CNPJ com 14 dígitos)", example = "12345678901")
        @NotBlank(message = "O documento (CPF/CNPJ) é obrigatório")
        String document,

        @Schema(description = "Tipo de pessoa (PF ou PJ)", example = "PF")
        @NotNull(message = "O tipo de documento (PF/PJ) é obrigatório")
        DocumentType documentType,

        @Schema(description = "Endereço de e-mail do titular", example = "matheus@mapbank.com")
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Telefone para contato com DDD", example = "+5511999998888")
        @NotBlank(message = "O telefone é obrigatório")
        String phone
) {}
