package com.mapbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbank.domain.enums.AccountType;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.domain.enums.PixKeyType;
import com.mapbank.dto.request.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Integração: Fluxo completo de Depósito, Transferência e PIX com Extrato")
    void testFullBankingTransactionLifecycle() throws Exception {
        // 1. Criar Cliente 1 (Matheus)
        CreateClientRequest clientReq1 = new CreateClientRequest("Matheus Araújo Pereira", "55566677788", DocumentType.PF, "map.it1@bank.com", "11999991111");
        String client1Json = mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(clientReq1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer client1Id = com.jayway.jsonpath.JsonPath.read(client1Json, "$.id");

        // 2. Criar Cliente 2 (NTT DATA)
        CreateClientRequest clientReq2 = new CreateClientRequest("NTT DATA Integration", "88899900011", DocumentType.PJ, "ntt.it2@bank.com", "11999992222");
        String client2Json = mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(clientReq2)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer client2Id = com.jayway.jsonpath.JsonPath.read(client2Json, "$.id");

        // 3. Abrir Conta 1 com R$ 2.000,00 de saldo inicial
        CreateAccountRequest accountReq1 = new CreateAccountRequest(Long.valueOf(client1Id), AccountType.CORRENTE, new BigDecimal("2000.00"), new BigDecimal("500.00"));
        String account1Json = mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(accountReq1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer account1Id = com.jayway.jsonpath.JsonPath.read(account1Json, "$.id");
        String account1Number = com.jayway.jsonpath.JsonPath.read(account1Json, "$.accountNumber");

        // 4. Abrir Conta 2 com R$ 100,00 de saldo inicial
        CreateAccountRequest accountReq2 = new CreateAccountRequest(Long.valueOf(client2Id), AccountType.CORRENTE, new BigDecimal("100.00"), BigDecimal.ZERO);
        String account2Json = mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(accountReq2)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer account2Id = com.jayway.jsonpath.JsonPath.read(account2Json, "$.id");
        String account2Number = com.jayway.jsonpath.JsonPath.read(account2Json, "$.accountNumber");

        // 5. Cadastrar chave PIX na Conta 2
        CreatePixKeyRequest pixReq = new CreatePixKeyRequest(PixKeyType.EMAIL, "financeiro.ntt@it.com");
        mockMvc.perform(post("/api/v1/pix/accounts/" + account2Id + "/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pixReq)))
                .andExpect(status().isCreated());

        // 6. Realizar Transferência Interna de R$ 500,00 da Conta 1 para a Conta 2
        InternalTransferRequest transferReq = new InternalTransferRequest(account1Number, account2Number, new BigDecimal("500.00"), "Serviço de TI");
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.amount", is(500.0)));

        // 7. Realizar Pagamento PIX de R$ 300,00 da Conta 1 para a chave da Conta 2
        PixPaymentRequest pixPayment = new PixPaymentRequest(account1Number, "financeiro.ntt@it.com", new BigDecimal("300.00"), "Consultoria");
        mockMvc.perform(post("/api/v1/transactions/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "PIX-IDEMPOTENT-001")
                        .content(objectMapper.writeValueAsString(pixPayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.transactionCode", is("PIX-IDEMPOTENT-001")));

        // 8. Consultar Extrato da Conta 1 (deve conter pelo menos 3 lançamentos: Aporte, Transferência e PIX)
        mockMvc.perform(get("/api/v1/transactions/accounts/" + account1Id + "/statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(3)));
    }
}
