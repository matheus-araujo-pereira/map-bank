package com.mapbank.controller;

import com.mapbank.dto.request.CreateAccountRequest;
import com.mapbank.dto.request.UpdateAccountLimitRequest;
import com.mapbank.dto.response.AccountResponse;
import com.mapbank.service.AccountService;
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
import java.util.List;

/**
 * Controller REST para abertura e gestão de contas bancárias no MAP-Bank.
 *
 * @author Matheus Araujo Pereira
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Contas Bancárias", description = "Endpoints para abertura, consulta de saldos, limites e encerramento de contas")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Abrir nova conta bancária", description = "Abre conta (Corrente, Poupança ou Salário) vinculada a cliente ativo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Conta bancária aberta com sucesso"),
            @ApiResponse(responseCode = "400", description = "Cliente inativo ou dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente não localizado")
    })
    public ResponseEntity<AccountResponse> openAccount(@RequestBody @Valid CreateAccountRequest request) {
        AccountResponse response = accountService.openAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar todas as contas", description = "Recupera lista paginada de contas cadastradas")
    public ResponseEntity<Page<AccountResponse>> listAccounts(@PageableDefault(size = 10, sort = "accountNumber") Pageable pageable) {
        return ResponseEntity.ok(accountService.listAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar conta por ID", description = "Retorna detalhes e saldo da conta pelo ID")
    public ResponseEntity<AccountResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.findById(id));
    }

    @GetMapping("/number/{accountNumber}")
    @Operation(summary = "Buscar conta por número", description = "Localiza a conta bancária pelo seu número com dígito")
    public ResponseEntity<AccountResponse> findByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.findByAccountNumber(accountNumber));
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Listar contas por cliente", description = "Retorna todas as contas ativas pertencentes ao cliente titular")
    public ResponseEntity<List<AccountResponse>> listByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(accountService.listByClient(clientId));
    }

    @PatchMapping("/{id}/limit")
    @Operation(summary = "Ajustar limite de cheque especial", description = "Atualiza o valor do limite pré-aprovado de cheque especial")
    public ResponseEntity<AccountResponse> updateLimit(@PathVariable Long id, @RequestBody @Valid UpdateAccountLimitRequest request) {
        return ResponseEntity.ok(accountService.updateOverdraftLimit(id, request));
    }

    @PatchMapping("/{id}/block")
    @Operation(summary = "Bloquear conta", description = "Bloqueia a conta para movimentações financeiras")
    public ResponseEntity<Void> blockAccount(@PathVariable Long id) {
        accountService.blockAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Encerrar conta bancária", description = "Encerra a conta (exige que o saldo esteja rigorosamente zerado)")
    public ResponseEntity<Void> closeAccount(@PathVariable Long id) {
        accountService.closeAccount(id);
        return ResponseEntity.noContent().build();
    }
}
