package com.mapbank.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Record (Java 21) para atualização dos dados cadastrais do cliente titular.
 *
 * @author Matheus Araujo Pereira
 */
@Schema(description = "Payload para atualização dos dados cadastrais do cliente")
public record UpdateClientRequest(
        @Schema(description = "Nome atualizado do cliente", example = "Matheus Araujo Pereira Atualizado")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String name,

        @Schema(description = "Email atualizado", example = "matheus.novo@mapbank.com")
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Telefone atualizado", example = "+5511988887777")
        @NotBlank(message = "O telefone é obrigatório")
        String phone
) {}
