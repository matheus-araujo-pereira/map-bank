package com.mapbank.controller;

import com.mapbank.dto.request.LoanContractRequest;
import com.mapbank.dto.request.LoanPaymentRequest;
import com.mapbank.dto.request.LoanSimulationRequest;
import com.mapbank.dto.response.LoanResponse;
import com.mapbank.dto.response.LoanSimulationResponse;
import com.mapbank.service.LoanService;
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
 * Controller REST para simulação, contratação e amortização de operações de crédito.
 *
 * @author Matheus Araujo Pereira
 */
@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Crédito e Empréstimos", description = "Endpoints para simulação Price, contratação com liberação de saldo e amortização")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simular empréstimo", description = "Calcula a projeção de parcelas e juros utilizando a Tabela Price")
    public ResponseEntity<LoanSimulationResponse> simulateLoan(@RequestBody @Valid LoanSimulationRequest request) {
        return ResponseEntity.ok(loanService.simulateLoan(request));
    }

    @PostMapping("/contract")
    @Operation(summary = "Contratar empréstimo", description = "Formaliza o contrato e credita o valor contratado imediatamente na conta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empréstimo contratado com sucesso e saldo liberado"),
            @ApiResponse(responseCode = "400", description = "Conta inativa ou dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Conta não localizada")
    })
    public ResponseEntity<LoanResponse> contractLoan(@RequestBody @Valid LoanContractRequest request) {
        LoanResponse response = loanService.contractLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/amortize")
    @Operation(summary = "Pagar / amortizar parcela", description = "Debita o valor da conta e amortiza o saldo devedor do contrato")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Amortização processada com sucesso"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente para o débito"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado")
    })
    public ResponseEntity<LoanResponse> amortizeLoan(@PathVariable Long id, @RequestBody @Valid LoanPaymentRequest request) {
        return ResponseEntity.ok(loanService.amortizeLoan(id, request));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Listar empréstimos da conta", description = "Recupera todos os contratos de crédito ativos da conta informada")
    public ResponseEntity<List<LoanResponse>> listByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(loanService.listByAccount(accountId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar contrato por ID", description = "Recupera dados detalhados e saldo devedor do contrato")
    public ResponseEntity<LoanResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.findById(id));
    }
}
