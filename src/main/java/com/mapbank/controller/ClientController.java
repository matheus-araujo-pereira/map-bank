package com.mapbank.controller;

import com.mapbank.dto.request.CreateClientRequest;
import com.mapbank.dto.request.UpdateClientRequest;
import com.mapbank.dto.response.ClientResponse;
import com.mapbank.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gestão do ciclo de vida dos clientes titulares do MAP-Bank.
 *
 * @author Matheus Araujo Pereira
 */
@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "Clientes", description = "Endpoints de gerenciamento e onboarding de clientes titulares (PF e PJ)")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo cliente titular", description = "Realiza o onboarding de Pessoa Física (CPF) ou Jurídica (CNPJ)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou documento/email já existente")
    })
    public ResponseEntity<ClientResponse> createClient(@RequestBody @Valid CreateClientRequest request) {
        ClientResponse response = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar clientes paginados", description = "Retorna lista de clientes com suporte a paginação e ordenação")
    public ResponseEntity<Page<ClientResponse>> listClients(@PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(clientService.listAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID", description = "Recupera dados cadastrais completos a partir do identificador único")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente localizado"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ResponseEntity<ClientResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @GetMapping("/document/{document}")
    @Operation(summary = "Buscar cliente por CPF ou CNPJ", description = "Localiza o cadastro do cliente pelo documento oficial")
    public ResponseEntity<ClientResponse> findByDocument(@PathVariable String document) {
        return ResponseEntity.ok(clientService.findByDocument(document));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados cadastrais", description = "Permite atualizar nome, e-mail e telefone do cliente")
    public ResponseEntity<ClientResponse> updateClient(@PathVariable Long id, @RequestBody @Valid UpdateClientRequest request) {
        return ResponseEntity.ok(clientService.updateClient(id, request));
    }

    @PatchMapping("/{id}/block")
    @Operation(summary = "Bloquear cliente", description = "Inativa temporariamente o cliente, bloqueando movimentações")
    public ResponseEntity<Void> blockClient(@PathVariable Long id) {
        clientService.blockClient(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Reativar cliente", description = "Restaura o status ativo do cliente")
    public ResponseEntity<Void> activateClient(@PathVariable Long id) {
        clientService.activateClient(id);
        return ResponseEntity.noContent().build();
    }
}
