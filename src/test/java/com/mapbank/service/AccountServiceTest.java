package com.mapbank.service;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.enums.AccountType;
import com.mapbank.domain.enums.ClientStatus;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.domain.model.Account;
import com.mapbank.domain.model.Client;
import com.mapbank.dto.request.CreateAccountRequest;
import com.mapbank.dto.request.UpdateAccountLimitRequest;
import com.mapbank.dto.response.AccountResponse;
import com.mapbank.exception.BusinessException;
import com.mapbank.repository.AccountRepository;
import com.mapbank.repository.ClientRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Client mockClient;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockClient = new Client("Matheus Araújo Pereira", "12345678901", DocumentType.PF, "matheus@mapbank.com", "+5511999998888");
        mockClient.setId(1L);

        mockAccount = new Account("10001-9", "0001", AccountType.CORRENTE, new BigDecimal("1000.00"), new BigDecimal("500.00"), mockClient);
        mockAccount.setId(10L);
    }

    @Test
    @DisplayName("Deve abrir conta bancária com sucesso quando cliente for ativo")
    void shouldOpenAccountSuccessfully() {
        CreateAccountRequest request = new CreateAccountRequest(1L, AccountType.CORRENTE, new BigDecimal("200.00"), new BigDecimal("1000.00"));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        AccountResponse response = accountService.openAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.accountNumber()).isEqualTo("10001-9");
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve impedir abertura de conta quando o cliente titular estiver inativo ou bloqueado")
    void shouldThrowExceptionWhenClientIsInactive() {
        mockClient.setStatus(ClientStatus.BLOCKED);
        CreateAccountRequest request = new CreateAccountRequest(1L, AccountType.CORRENTE, BigDecimal.ZERO, BigDecimal.ZERO);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));

        assertThatThrownBy(() -> accountService.openAccount(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é permitido abrir conta para cliente inativo ou bloqueado");
    }

    @Test
    @DisplayName("Deve impedir encerramento de conta com saldo positivo ou negativo")
    void shouldPreventClosingAccountWithNonZeroBalance() {
        mockAccount.setBalance(new BigDecimal("150.00"));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(mockAccount));

        assertThatThrownBy(() -> accountService.closeAccount(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exatamente zerado");
    }

    @Test
    @DisplayName("Deve encerrar conta bancária com sucesso quando o saldo estiver zerado")
    void shouldCloseAccountWhenBalanceIsZero() {
        mockAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        accountService.closeAccount(10L);

        assertThat(mockAccount.getStatus()).isEqualTo(AccountStatus.CLOSED);
        verify(accountRepository, times(1)).save(mockAccount);
    }

    @Test
    @DisplayName("Deve atualizar limite de cheque especial com sucesso")
    void shouldUpdateOverdraftLimit() {
        UpdateAccountLimitRequest request = new UpdateAccountLimitRequest(new BigDecimal("2500.00"));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        AccountResponse response = accountService.updateOverdraftLimit(10L, request);

        assertThat(mockAccount.getOverdraftLimit()).isEqualByComparingTo("2500.00");
    }
}
