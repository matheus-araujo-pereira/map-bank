package com.mapbank.service;

import com.mapbank.domain.enums.ClientStatus;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.domain.model.Client;
import com.mapbank.dto.request.CreateClientRequest;
import com.mapbank.dto.response.ClientResponse;
import com.mapbank.exception.BusinessException;
import com.mapbank.exception.ResourceNotFoundException;
import com.mapbank.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    private CreateClientRequest validRequest;
    private Client mockClient;

    @BeforeEach
    void setUp() {
        validRequest = new CreateClientRequest(
                "Matheus Araújo Pereira",
                "12345678901",
                DocumentType.PF,
                "matheus@mapbank.com",
                "+5511999998888"
        );

        mockClient = new Client("Matheus Araújo Pereira", "12345678901", DocumentType.PF, "matheus@mapbank.com", "+5511999998888");
        mockClient.setId(1L);
    }

    @Test
    @DisplayName("Deve cadastrar cliente titular com sucesso quando documento e email forem inéditos")
    void shouldCreateClientSuccessfully() {
        when(clientRepository.existsByDocument(validRequest.document())).thenReturn(false);
        when(clientRepository.existsByEmail(validRequest.email())).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenReturn(mockClient);

        ClientResponse response = clientService.createClient(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Matheus Araújo Pereira");
        assertThat(response.document()).isEqualTo("12345678901");
        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando CPF/CNPJ já estiver cadastrado")
    void shouldThrowExceptionWhenDocumentAlreadyExists() {
        when(clientRepository.existsByDocument(validRequest.document())).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um cliente titular cadastrado com este documento");

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando e-mail já estiver cadastrado")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(clientRepository.existsByDocument(validRequest.document())).thenReturn(false);
        when(clientRepository.existsByEmail(validRequest.email())).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um cliente titular cadastrado com este endereço de e-mail");

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    @DisplayName("Deve bloquear e reativar cliente com sucesso")
    void shouldBlockAndActivateClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(mockClient));
        when(clientRepository.save(any(Client.class))).thenReturn(mockClient);

        clientService.blockClient(1L);
        assertThat(mockClient.getStatus()).isEqualTo(ClientStatus.BLOCKED);

        clientService.activateClient(1L);
        assertThat(mockClient.getStatus()).isEqualTo(ClientStatus.ACTIVE);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando cliente não for localizado")
    void shouldThrowNotFoundException() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
