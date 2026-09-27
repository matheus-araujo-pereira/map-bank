package com.mapbank.service;

import com.mapbank.domain.enums.ClientStatus;
import com.mapbank.domain.model.Client;
import com.mapbank.dto.request.CreateClientRequest;
import com.mapbank.dto.request.UpdateClientRequest;
import com.mapbank.dto.response.ClientResponse;
import com.mapbank.exception.BusinessException;
import com.mapbank.exception.ResourceNotFoundException;
import com.mapbank.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de gerenciamento do ciclo de vida de clientes titulares do MAP-Bank.
 *
 * @author Matheus Araujo Pereira
 */
@Service
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * Cadastra um novo cliente titular, assegurando a unicidade de CPF/CNPJ e e-mail.
     *
     * @param request dados cadastrais do novo cliente.
     * @return {@link ClientResponse} dados persistidos.
     */
    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        log.info("Iniciando cadastro de cliente com documento: {}", request.document());

        if (clientRepository.existsByDocument(request.document())) {
            throw new BusinessException("Já existe um cliente titular cadastrado com este documento (CPF/CNPJ).");
        }
        if (clientRepository.existsByEmail(request.email())) {
            throw new BusinessException("Já existe um cliente titular cadastrado com este endereço de e-mail.");
        }

        Client client = new Client(
                request.name(),
                request.document(),
                request.documentType(),
                request.email(),
                request.phone()
        );

        Client saved = clientRepository.save(client);
        log.info("Cliente titular cadastrado com sucesso. ID: {}", saved.getId());
        return ClientResponse.fromEntity(saved);
    }

    /**
     * Busca um cliente pelo seu identificador primário.
     */
    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não localizado para o ID: " + id));
        return ClientResponse.fromEntity(client);
    }

    /**
     * Busca um cliente pelo seu documento (CPF/CNPJ).
     */
    @Transactional(readOnly = true)
    public ClientResponse findByDocument(String document) {
        Client client = clientRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não localizado para o documento: " + document));
        return ClientResponse.fromEntity(client);
    }

    /**
     * Listagem paginada de todos os clientes titulares.
     */
    @Transactional(readOnly = true)
    public Page<ClientResponse> listAll(Pageable pageable) {
        return clientRepository.findAll(pageable).map(ClientResponse::fromEntity);
    }

    /**
     * Atualiza os dados cadastrais (nome, email, telefone) do cliente titular.
     */
    @Transactional
    public ClientResponse updateClient(Long id, UpdateClientRequest request) {
        log.info("Atualizando dados do cliente ID: {}", id);
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não localizado para o ID: " + id));

        // Se o email mudou, verifica se o novo já não está em uso
        if (!client.getEmail().equalsIgnoreCase(request.email()) && clientRepository.existsByEmail(request.email())) {
            throw new BusinessException("O e-mail informado já está em uso por outro cliente.");
        }

        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());

        Client updated = clientRepository.save(client);
        return ClientResponse.fromEntity(updated);
    }

    /**
     * Bloqueia o cadastro do cliente, impedindo novas operações financeiras.
     */
    @Transactional
    public void blockClient(Long id) {
        log.warn("Bloqueando cliente ID: {}", id);
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não localizado para o ID: " + id));
        client.setStatus(ClientStatus.BLOCKED);
        clientRepository.save(client);
    }

    /**
     * Reativa o cadastro do cliente.
     */
    @Transactional
    public void activateClient(Long id) {
        log.info("Reativando cliente ID: {}", id);
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não localizado para o ID: " + id));
        client.setStatus(ClientStatus.ACTIVE);
        clientRepository.save(client);
    }
}
