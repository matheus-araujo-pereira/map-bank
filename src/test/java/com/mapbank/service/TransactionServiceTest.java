package com.mapbank.service;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.enums.AccountType;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.domain.enums.PixKeyType;
import com.mapbank.domain.model.Account;
import com.mapbank.domain.model.Client;
import com.mapbank.domain.model.PixKey;
import com.mapbank.domain.model.Transaction;
import com.mapbank.dto.request.DepositRequest;
import com.mapbank.dto.request.InternalTransferRequest;
import com.mapbank.dto.request.PixPaymentRequest;
import com.mapbank.dto.request.WithdrawRequest;
import com.mapbank.dto.response.TransactionResponse;
import com.mapbank.exception.InsufficientBalanceException;
import com.mapbank.repository.AccountRepository;
import com.mapbank.repository.PixKeyRepository;
import com.mapbank.repository.TransactionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PixKeyRepository pixKeyRepository;

    private TransactionService transactionService;

    private Account sourceAccount;
    private Account targetAccount;
    private Client clientSource;
    private Client clientTarget;

    @BeforeEach
    void setUp() {
        // SimpleMeterRegistry em memória para testar métricas Micrometer
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        transactionService = new TransactionService(transactionRepository, accountRepository, pixKeyRepository, meterRegistry);

        clientSource = new Client("Matheus Araújo Pereira", "12345678901", DocumentType.PF, "matheus@mapbank.com", "+5511999998888");
        clientSource.setId(1L);

        clientTarget = new Client("NTT DATA Brasil", "11222333000199", DocumentType.PJ, "contato@nttdata.com", "+551130001000");
        clientTarget.setId(2L);

        sourceAccount = new Account("10001-9", "0001", AccountType.CORRENTE, new BigDecimal("1000.00"), new BigDecimal("500.00"), clientSource);
        sourceAccount.setId(10L);

        targetAccount = new Account("20002-8", "0001", AccountType.CORRENTE, new BigDecimal("200.00"), BigDecimal.ZERO, clientTarget);
        targetAccount.setId(20L);
    }

    @Test
    @DisplayName("Deve processar depósito financeiro com sucesso e creditar saldo")
    void shouldProcessDepositSuccessfully() {
        DepositRequest request = new DepositRequest(new BigDecimal("300.00"), "Depósito via PIX");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(sourceAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.processDeposit(10L, request);

        assertThat(response).isNotNull();
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("1300.00");
        verify(accountRepository, times(1)).save(sourceAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deve processar saque com sucesso debitando do saldo")
    void shouldProcessWithdrawalSuccessfully() {
        WithdrawRequest request = new WithdrawRequest(new BigDecimal("400.00"), "Saque ATM");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(sourceAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.processWithdrawal(10L, request);

        assertThat(response).isNotNull();
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("600.00");
        verify(accountRepository, times(1)).save(sourceAccount);
    }

    @Test
    @DisplayName("Deve lançar InsufficientBalanceException quando saldo e limite forem insuficientes para o saque")
    void shouldThrowInsufficientBalanceOnWithdrawal() {
        WithdrawRequest request = new WithdrawRequest(new BigDecimal("2000.00"), "Saque alto");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> transactionService.processWithdrawal(10L, request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(accountRepository, never()).save(sourceAccount);
    }

    @Test
    @DisplayName("Deve realizar transferência interna entre contas debitando da origem e creditando no destino")
    void shouldProcessInternalTransferSuccessfully() {
        InternalTransferRequest request = new InternalTransferRequest("10001-9", "20002-8", new BigDecimal("500.00"), "Pagamento");
        when(accountRepository.findByAccountNumber("10001-9")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("20002-8")).thenReturn(Optional.of(targetAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.processInternalTransfer(request, null);

        assertThat(response).isNotNull();
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("500.00");
        assertThat(targetAccount.getBalance()).isEqualByComparingTo("700.00");
        verify(accountRepository, times(1)).save(sourceAccount);
        verify(accountRepository, times(1)).save(targetAccount);
    }

    @Test
    @DisplayName("Deve realizar transferência PIX com chave de endereçamento cadastrada")
    void shouldProcessPixTransferSuccessfully() {
        PixPaymentRequest request = new PixPaymentRequest("10001-9", "contato@nttdata.com", new BigDecimal("250.00"), "PIX consultoria");
        PixKey targetPixKey = new PixKey("contato@nttdata.com", PixKeyType.EMAIL, targetAccount);

        when(accountRepository.findByAccountNumber("10001-9")).thenReturn(Optional.of(sourceAccount));
        when(pixKeyRepository.findByKeyValue("contato@nttdata.com")).thenReturn(Optional.of(targetPixKey));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.processPixTransfer(request, null);

        assertThat(response).isNotNull();
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("750.00");
        assertThat(targetAccount.getBalance()).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("Deve garantir idempotência retornando a mesma transação caso a Idempotency-Key já exista")
    void shouldRespectIdempotencyKey() {
        InternalTransferRequest request = new InternalTransferRequest("10001-9", "20002-8", new BigDecimal("100.00"), "Idempotente");
        Transaction existingTx = new Transaction("IDEMPOTENT-123", sourceAccount, targetAccount, new BigDecimal("100.00"),
                null, null, "Transação prévia");

        when(transactionRepository.findByTransactionCode("IDEMPOTENT-123")).thenReturn(Optional.of(existingTx));

        TransactionResponse response = transactionService.processInternalTransfer(request, "IDEMPOTENT-123");

        assertThat(response.transactionCode()).isEqualTo("IDEMPOTENT-123");
        verify(accountRepository, never()).findByAccountNumber(anyString());
    }
}
