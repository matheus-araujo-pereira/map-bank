package com.mapbank.service;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.domain.model.Account;
import com.mapbank.domain.model.PixKey;
import com.mapbank.dto.request.CreatePixKeyRequest;
import com.mapbank.dto.response.PixKeyResponse;
import com.mapbank.exception.BusinessException;
import com.mapbank.exception.ResourceNotFoundException;
import com.mapbank.repository.AccountRepository;
import com.mapbank.repository.PixKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Serviço de gerenciamento do ecossistema de chaves de endereçamento PIX (BACEN DICT).
 *
 * @author Matheus Araujo Pereira
 */
@Service
public class PixService {

    private static final Logger log = LoggerFactory.getLogger(PixService.class);

    private final PixKeyRepository pixKeyRepository;
    private final AccountRepository accountRepository;

    public PixService(PixKeyRepository pixKeyRepository, AccountRepository accountRepository) {
        this.pixKeyRepository = pixKeyRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Registra uma nova chave PIX vinculada a uma conta bancária ativa,
     * respeitando as regras regulatórias do Banco Central (limite de 5 chaves para PF e 20 para PJ).
     *
     * @param accountId identificador da conta bancária.
     * @param request dados da chave PIX.
     * @return {@link PixKeyResponse} com os dados da chave cadastrada.
     */
    @Transactional
    public PixKeyResponse registerPixKey(Long accountId, CreatePixKeyRequest request) {
        log.info("Cadastrando chave PIX do tipo {} para a conta ID: {}", request.keyType(), accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada para o ID: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Apenas contas ativas podem cadastrar chaves PIX.");
        }

        // Validação de unicidade no diretório de chaves
        if (pixKeyRepository.existsByKeyValue(request.keyValue())) {
            throw new BusinessException("Esta chave PIX já se encontra cadastrada e vinculada a uma conta bancária.");
        }

        // Validação dos limites de chaves regulatórios do BACEN
        long totalKeys = pixKeyRepository.countByAccountId(accountId);
        DocumentType docType = account.getClient().getDocumentType();
        int maxAllowed = (docType == DocumentType.PF) ? 5 : 20;

        if (totalKeys >= maxAllowed) {
            throw new BusinessException("Limite máximo de chaves PIX atingido para titular " + docType + " (" + maxAllowed + " chaves).");
        }

        PixKey pixKey = new PixKey(request.keyValue(), request.keyType(), account);
        PixKey saved = pixKeyRepository.save(pixKey);

        log.info("Chave PIX {} cadastrada com sucesso para a conta {}", saved.getKeyValue(), account.getAccountNumber());
        return PixKeyResponse.fromEntity(saved);
    }

    /**
     * Localiza os dados de uma chave PIX e sua conta correspondente.
     */
    @Transactional(readOnly = true)
    public PixKeyResponse findByKey(String keyValue) {
        PixKey pixKey = pixKeyRepository.findByKeyValue(keyValue)
                .orElseThrow(() -> new ResourceNotFoundException("Chave PIX não localizada no DICT: " + keyValue));
        return PixKeyResponse.fromEntity(pixKey);
    }

    /**
     * Lista todas as chaves PIX ativas de uma conta.
     */
    @Transactional(readOnly = true)
    public List<PixKeyResponse> listKeysByAccount(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Conta bancária não localizada para o ID: " + accountId);
        }
        return pixKeyRepository.findByAccountId(accountId).stream()
                .map(PixKeyResponse::fromEntity)
                .toList();
    }

    /**
     * Remove / desvincula uma chave PIX.
     */
    @Transactional
    public void deletePixKey(Long keyId) {
        log.info("Desvinculando chave PIX ID: {}", keyId);
        PixKey pixKey = pixKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("Chave PIX não localizada para o ID: " + keyId));
        pixKeyRepository.delete(pixKey);
    }
}
