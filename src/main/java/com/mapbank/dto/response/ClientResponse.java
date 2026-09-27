package com.mapbank.dto.response;

import com.mapbank.domain.enums.ClientStatus;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.domain.model.Client;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Record (Java 21) representando os dados consolidados de um cliente titular.
 *
 * @author Matheus Araújo Pereira
 */
@Schema(description = "Dados detalhados do cliente titular")
public record ClientResponse(
        @Schema(description = "Identificador único do cliente", example = "1")
        Long id,

        @Schema(description = "Nome completo ou Razão Social", example = "Matheus Araújo Pereira")
        String name,

        @Schema(description = "Documento cadastrado (CPF/CNPJ)", example = "12345678901")
        String document,

        @Schema(description = "Tipo de documento (PF/PJ)", example = "PF")
        DocumentType documentType,

        @Schema(description = "E-mail de contato", example = "matheus@mapbank.com")
        String email,

        @Schema(description = "Telefone cadastrado", example = "+5511999998888")
        String phone,

        @Schema(description = "Situação cadastral", example = "ACTIVE")
        ClientStatus status,

        @Schema(description = "Data e hora do cadastro")
        Instant createdAt,

        @Schema(description = "Data da última atualização cadastral")
        Instant updatedAt
) {
    public static ClientResponse fromEntity(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getDocument(),
                client.getDocumentType(),
                client.getEmail(),
                client.getPhone(),
                client.getStatus(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
