package com.mapbank.service;

import com.mapbank.domain.enums.AccountType;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.domain.enums.LoanStatus;
import com.mapbank.domain.model.Account;
import com.mapbank.domain.model.Client;
import com.mapbank.domain.model.Loan;
import com.mapbank.dto.request.LoanContractRequest;
import com.mapbank.dto.request.LoanPaymentRequest;
import com.mapbank.dto.request.LoanSimulationRequest;
import com.mapbank.dto.response.LoanResponse;
import com.mapbank.dto.response.LoanSimulationResponse;
import com.mapbank.repository.AccountRepository;
import com.mapbank.repository.LoanRepository;
import com.mapbank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private LoanService loanService;

    private Account mockAccount;
    private Client mockClient;

    @BeforeEach
    void setUp() {
        mockClient = new Client("Matheus Araujo Pereira", "12345678901", DocumentType.PF, "matheus@mapbank.com", "+5511999998888");
        mockAccount = new Account("10001-9", "0001", AccountType.CORRENTE, new BigDecimal("1000.00"), BigDecimal.ZERO, mockClient);
        mockAccount.setId(10L);
    }

    @Test
    @DisplayName("Deve simular empréstimo calculando parcelas da Tabela Price com taxa mensal")
    void shouldSimulateLoanUsingPriceTable() {
        LoanSimulationRequest request = new LoanSimulationRequest(new BigDecimal("10000.00"), 12);

        LoanSimulationResponse response = loanService.simulateLoan(request);

        assertThat(response).isNotNull();
        assertThat(response.requestedAmount()).isEqualByComparingTo("10000.00");
        assertThat(response.installments()).isEqualTo(12);
        assertThat(response.installmentAmount()).isGreaterThan(new BigDecimal("800.00"));
        assertThat(response.totalAmount()).isGreaterThan(new BigDecimal("10000.00"));
        assertThat(response.totalInterest()).isPositive();
    }

    @Test
    @DisplayName("Deve contratar empréstimo e creditar o valor contratado imediatamente no saldo da conta")
    void shouldContractLoanAndDisburseBalance() {
        LoanContractRequest request = new LoanContractRequest("10001-9", new BigDecimal("5000.00"), 6);

        when(accountRepository.findByAccountNumber("10001-9")).thenReturn(Optional.of(mockAccount));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponse response = loanService.contractLoan(request);

        assertThat(response).isNotNull();
        assertThat(response.requestedAmount()).isEqualByComparingTo("5000.00");
        // O saldo inicial de 1000.00 somado aos 5000.00 de crédito liberado resulta em 6000.00
        assertThat(mockAccount.getBalance()).isEqualByComparingTo("6000.00");
        verify(accountRepository, times(1)).save(mockAccount);
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve amortizar parcela de empréstimo debitando da conta e reduzindo o saldo devedor")
    void shouldAmortizeLoanAndReduceBalance() {
        Loan loan = new Loan("LN-1234", mockAccount, new BigDecimal("5000.00"), new BigDecimal("0.0250"),
                6, new BigDecimal("907.00"), new BigDecimal("5442.00"));
        loan.setId(1L);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanPaymentRequest paymentRequest = new LoanPaymentRequest(new BigDecimal("907.00"));
        LoanResponse response = loanService.amortizeLoan(1L, paymentRequest);

        assertThat(response).isNotNull();
        assertThat(mockAccount.getBalance()).isEqualByComparingTo("93.00"); // 1000 - 907
        assertThat(loan.getRemainingBalance()).isEqualByComparingTo("4535.00"); // 5442 - 907
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPROVED);
    }
}
