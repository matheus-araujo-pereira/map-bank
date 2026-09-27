package com.mapbank.controller;

import com.mapbank.dto.request.DepositRequest;
import com.mapbank.dto.request.InternalTransferRequest;
import com.mapbank.dto.request.PixPaymentRequest;
import com.mapbank.dto.request.WithdrawRequest;
import com.mapbank.dto.response.TransactionResponse;
import com.mapbank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

/**
 * Controller REST para liquidação de transações financeiras e consultas de extrato contábil.
 *
 * @author Matheus Araujo Pereira
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transações Financeiras", description = "Endpoints de processamento de depósitos, saques, transferências internas, PIX e extrato")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/accounts/{accountId}/deposit")
    @Operation(summary = "Realizar depósito", description = "Credita recursos monetários na conta bancária indicada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Depósito creditado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Conta inativa ou valor inválido"),
            @ApiResponse(responseCode = "404", description = "Conta não localizada")
    })
    public ResponseEntity<TransactionResponse> deposit(@PathVariable Long accountId,
                                                        @RequestBody @Valid DepositRequest request) {
        return ResponseEntity.ok(transactionService.processDeposit(accountId, request));
    }

    @PostMapping("/accounts/{accountId}/withdraw")
    @Operation(summary = "Realizar saque", description = "Debita recursos da conta com validação estrita de saldo e limite")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saque efetuado com sucesso"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente"),
            @ApiResponse(responseCode = "404", description = "Conta não localizada")
    })
    public ResponseEntity<TransactionResponse> withdraw(@PathVariable Long accountId,
                                                         @RequestBody @Valid WithdrawRequest request) {
        return ResponseEntity.ok(transactionService.processWithdrawal(accountId, request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transferência entre contas", description = "Transfere recursos entre contas do MAP-Bank com suporte a chave de idempotência")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transferência liquidada com sucesso"),
            @ApiResponse(responseCode = "409", description = "Conflito de concorrência (Optimistic Lock)"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    public ResponseEntity<TransactionResponse> transfer(
            @RequestBody @Valid InternalTransferRequest request,
            @Parameter(description = "Chave de Idempotência (UUID) para evitar débitos duplicados")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(transactionService.processInternalTransfer(request, idempotencyKey));
    }

    @PostMapping("/pix")
    @Operation(summary = "Transferência instantânea PIX", description = "Liquida transferência via chave PIX com auditoria no SPI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PIX liquidado com sucesso"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente na conta pagadora"),
            @ApiResponse(responseCode = "404", description = "Chave PIX ou conta pagadora não encontrada")
    })
    public ResponseEntity<TransactionResponse> pixTransfer(
            @RequestBody @Valid PixPaymentRequest request,
            @Parameter(description = "Chave de Idempotência (UUID)")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(transactionService.processPixTransfer(request, idempotencyKey));
    }

    @GetMapping("/accounts/{accountId}/statement")
    @Operation(summary = "Consultar extrato bancário", description = "Retorna histórico contábil detalhado de débitos e créditos com filtro por período")
    public ResponseEntity<Page<TransactionResponse>> getStatement(
            @PathVariable Long accountId,
            @Parameter(description = "Data inicial (ISO-8601)", example = "2026-09-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Data final (ISO-8601)", example = "2026-09-30T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAccountStatement(accountId, startDate, endDate, pageable));
    }
}
