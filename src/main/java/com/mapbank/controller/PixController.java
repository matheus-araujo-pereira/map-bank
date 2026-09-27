package com.mapbank.controller;

import com.mapbank.dto.request.CreatePixKeyRequest;
import com.mapbank.dto.response.PixKeyResponse;
import com.mapbank.service.PixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller REST para o ecossistema de Chaves PIX (DICT) do MAP-Bank.
 *
 * @author Matheus Araújo Pereira
 */
@RestController
@RequestMapping("/api/v1/pix")
@Tag(name = "Chaves PIX", description = "Endpoints para registro, consulta e desvinculação de chaves PIX (BACEN DICT)")
public class PixController {

    private final PixService pixService;

    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping("/accounts/{accountId}/keys")
    @Operation(summary = "Cadastrar chave PIX", description = "Vincula uma chave (CPF, CNPJ, Email, Telefone ou Aleatória) à conta bancária")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chave PIX registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Chave já cadastrada ou limite de chaves excedido"),
            @ApiResponse(responseCode = "404", description = "Conta bancária não encontrada")
    })
    public ResponseEntity<PixKeyResponse> registerPixKey(@PathVariable Long accountId,
                                                         @RequestBody @Valid CreatePixKeyRequest request) {
        PixKeyResponse response = pixService.registerPixKey(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/keys/{keyValue}")
    @Operation(summary = "Consultar chave no DICT", description = "Busca a chave PIX e os dados da respectiva conta bancária recebedora")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chave PIX localizada"),
            @ApiResponse(responseCode = "404", description = "Chave não encontrada no diretório")
    })
    public ResponseEntity<PixKeyResponse> findByKey(@PathVariable String keyValue) {
        return ResponseEntity.ok(pixService.findByKey(keyValue));
    }

    @GetMapping("/accounts/{accountId}/keys")
    @Operation(summary = "Listar chaves da conta", description = "Retorna todas as chaves PIX ativas associadas à conta especificada")
    public ResponseEntity<List<PixKeyResponse>> listKeysByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(pixService.listKeysByAccount(accountId));
    }

    @DeleteMapping("/keys/{keyId}")
    @Operation(summary = "Excluir chave PIX", description = "Remove e desvincula a chave PIX do diretório")
    public ResponseEntity<Void> deletePixKey(@PathVariable Long keyId) {
        pixService.deletePixKey(keyId);
        return ResponseEntity.noContent().build();
    }
}
