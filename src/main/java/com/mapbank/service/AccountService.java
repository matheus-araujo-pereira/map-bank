package com.mapbank.service;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.enums.ClientStatus;
import com.mapbank.domain.enums.TransactionStatus;
import com.mapbank.domain.enums.TransactionType;
import com.mapbank.domain.model.Account;
import com.mapbank.domain.model.Client;
import com.mapbank.domain.model.Transaction;
import com.mapbank.dto.request.CreateAccountRequest;
import com.mapbank.dto.request.UpdateAccountLimitRequest;
import com.mapbank.dto.response.AccountResponse;
import com.mapbank.exception.BusinessException;
import com.mapbank.exception.ResourceNotFoundException;
import com.mapbank.repository.AccountRepository;
import com.mapbank.repository.ClientRepository;
import com.mapbank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Serviço de gestão de contas bancárias, saldos e limites do MAP-Bank.
 *
 * @author Matheus Araújo Pereira
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          ClientRepository clientRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Realiza a abertura de uma nova conta bancária para um cliente cadastrado e ativo.
     *
     * @param request dados da conta e titular.
     * @return {@link AccountResponse} com os dados da conta aberta.
     */
    @Transactional
    public AccountResponse openAccount(CreateAccountRequest request) {
        log.info("Iniciando abertura de conta para cliente ID: {}", request.clientId());

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente titular não localizado para o ID: " + request.clientId()));

        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new BusinessException("Não é permitido abrir conta para cliente inativo ou bloqueado.");
        }

        String accountNumber = generateUniqueAccountNumber();
        BigDecimal initialDeposit = request.initialDeposit() != null ? request.initialDeposit() : BigDecimal.ZERO;
        BigDecimal overdraft = request.overdraftLimit() != null ? request.overdraftLimit() : BigDecimal.ZERO;

        Account account = new Account(
                accountNumber,
                "0001",
                request.accountType(),
                initialDeposit,
                overdraft,
                client
        );

        Account savedAccount = accountRepository.save(account);

        // Se houver depósito inicial, gera o registro no Ledger contábil
        if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            Transaction initialTx = new Transaction(
                    "DEP-INIT-" + UUID.randomUUID().toString().substring(0, 8),
                    null,
                    savedAccount,
                    initialDeposit,
                    TransactionType.DEPOSIT,
                    TransactionStatus.COMPLETED,
                    "Aporte de abertura de conta"
            );
            transactionRepository.save(initialTx);
        }

        log.info("Conta bancária {} aberta com sucesso para o cliente {}", savedAccount.getAccountNumber(), client.getName());
        return AccountResponse.fromEntity(savedAccount);
    }

    /**
     * Localiza uma conta bancária pelo seu identificador primário.
     */
    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada para o ID: " + id));
        return AccountResponse.fromEntity(account);
    }

    /**
     * Localiza uma conta bancária pelo número com dígito.
     */
    @Transactional(readOnly = true)
    public AccountResponse findByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada para o número: " + accountNumber));
        return AccountResponse.fromEntity(account);
    }

    /**
     * Lista todas as contas bancárias de forma paginada.
     */
    @Transactional(readOnly = true)
    public Page<AccountResponse> listAll(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AccountResponse::fromEntity);
    }

    /**
     * Lista as contas de um determinado cliente.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> listByClient(Long clientId) {
        return accountRepository.findByClientId(clientId).stream()
                .map(AccountResponse::fromEntity)
                .toList();
    }

    /**
     * Atualiza o limite de cheque especial contratado para a conta bancária.
     */
    @Transactional
    public AccountResponse updateOverdraftLimit(Long id, UpdateAccountLimitRequest request) {
        log.info("Atualizando limite de cheque especial da conta ID: {} para R$ {}", id, request.overdraftLimit());
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada para o ID: " + id));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Apenas contas ativas podem ter o limite de cheque especial alterado.");
        }

        account.setOverdraftLimit(request.overdraftLimit());
        Account updated = accountRepository.save(account);
        return AccountResponse.fromEntity(updated);
    }

    /**
     * Bloqueia preventivamente a conta bancária (ex: suspeita de fraude).
     */
    @Transactional
    public void blockAccount(Long id) {
        log.warn("Bloqueando preventivamente conta ID: {}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada para o ID: " + id));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
    }

    /**
     * Encerra a conta bancária, exigindo que o saldo seja rigorosamente zero.
     */
    @Transactional
    public void closeAccount(Long id) {
        log.info("Solicitação de encerramento da conta ID: {}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada para o ID: " + id));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("A conta bancária só pode ser encerrada se o saldo estiver exatamente zerado. Saldo atual: R$ " + account.getBalance());
        }

        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        log.info("Conta {} encerrada com sucesso.", account.getAccountNumber());
    }

    /**
     * Gera um número único de conta bancária com dígito verificador.
     */
    private String generateUniqueAccountNumber() {
        String candidateNumber;
        do {
            int baseNumber = ThreadLocalRandom.current().nextInt(10000, 99999);
            int checkDigit = baseNumber % 9;
            candidateNumber = baseNumber + "-" + checkDigit;
        } while (accountRepository.existsByAccountNumber(candidateNumber));
        return candidateNumber;
    }
}
