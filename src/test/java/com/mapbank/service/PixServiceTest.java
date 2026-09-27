package com.mapbank.service;

import com.mapbank.domain.enums.AccountType;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.domain.enums.PixKeyType;
import com.mapbank.domain.model.Account;
import com.mapbank.domain.model.Client;
import com.mapbank.domain.model.PixKey;
import com.mapbank.dto.request.CreatePixKeyRequest;
import com.mapbank.dto.response.PixKeyResponse;
import com.mapbank.exception.BusinessException;
import com.mapbank.repository.AccountRepository;
import com.mapbank.repository.PixKeyRepository;
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
class PixServiceTest {

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private PixService pixService;

    private Account mockAccount;
    private Client mockClient;

    @BeforeEach
    void setUp() {
        mockClient = new Client("Matheus Araujo Pereira", "12345678901", DocumentType.PF, "matheus@mapbank.com", "+5511999998888");
        mockAccount = new Account("10001-9", "0001", AccountType.CORRENTE, new BigDecimal("1000.00"), BigDecimal.ZERO, mockClient);
        mockAccount.setId(10L);
    }

    @Test
    @DisplayName("Deve cadastrar chave PIX com sucesso dentro dos limites regulatórios")
    void shouldRegisterPixKeySuccessfully() {
        CreatePixKeyRequest request = new CreatePixKeyRequest(PixKeyType.EMAIL, "matheus@mapbank.com");
        PixKey savedPixKey = new PixKey("matheus@mapbank.com", PixKeyType.EMAIL, mockAccount);
        savedPixKey.setId(1L);

        when(accountRepository.findById(10L)).thenReturn(Optional.of(mockAccount));
        when(pixKeyRepository.existsByKeyValue("matheus@mapbank.com")).thenReturn(false);
        when(pixKeyRepository.countByAccountId(10L)).thenReturn(2L); // 2 < 5
        when(pixKeyRepository.save(any(PixKey.class))).thenReturn(savedPixKey);

        PixKeyResponse response = pixService.registerPixKey(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.keyValue()).isEqualTo("matheus@mapbank.com");
        verify(pixKeyRepository, times(1)).save(any(PixKey.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando a chave PIX já existir no DICT")
    void shouldThrowExceptionWhenKeyAlreadyRegistered() {
        CreatePixKeyRequest request = new CreatePixKeyRequest(PixKeyType.EMAIL, "matheus@mapbank.com");

        when(accountRepository.findById(10L)).thenReturn(Optional.of(mockAccount));
        when(pixKeyRepository.existsByKeyValue("matheus@mapbank.com")).thenReturn(true);

        assertThatThrownBy(() -> pixService.registerPixKey(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Esta chave PIX já se encontra cadastrada");

        verify(pixKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve barrar cadastro se titular PF atingir o limite de 5 chaves")
    void shouldThrowExceptionWhenExceedingMaxKeysForPF() {
        CreatePixKeyRequest request = new CreatePixKeyRequest(PixKeyType.PHONE, "+5511988887777");

        when(accountRepository.findById(10L)).thenReturn(Optional.of(mockAccount));
        when(pixKeyRepository.existsByKeyValue(anyString())).thenReturn(false);
        when(pixKeyRepository.countByAccountId(10L)).thenReturn(5L); // limite atingido

        assertThatThrownBy(() -> pixService.registerPixKey(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Limite máximo de chaves PIX atingido");
    }
}
