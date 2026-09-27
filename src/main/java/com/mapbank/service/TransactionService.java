package com.mapbank.service;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.enums.TransactionStatus;
import com.mapbank.domain.enums.TransactionType;
import com.mapbank.domain.model.Account;
import com.mapbank.domain.model.PixKey;
import com.mapbank.domain.model.Transaction;
import com.mapbank.dto.request.DepositRequest;
import com.mapbank.dto.request.InternalTransferRequest;
import com.mapbank.dto.request.PixPaymentRequest;
import com.mapbank.dto.request.WithdrawRequest;
import com.mapbank.dto.response.TransactionResponse;
import com.mapbank.exception.BusinessException;
import com.mapbank.exception.InsufficientBalanceException;
import com.mapbank.exception.ResourceNotFoundException;
import com.mapbank.repository.AccountRepository;
import com.mapbank.repository.PixKeyRepository;
import com.mapbank.repository.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Motor central de processamento financeiro e transacional do MAP-Bank.
 *
 * <p>Implementa atomicidade transacional com {@link Transactional}, tratamento de concorrência
 * via <b>Optimistic Locking</b> nas contas e instrumentação de métricas corporativas
 * através do <b>Micrometer (Prometheus)</b>.</p>
 *
 * @author Matheus Araujo Pereira
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PixKeyRepository pixKeyRepository;

    // Métricas de Negócio customizadas (Micrometer / Prometheus)
    private final Counter pixTransfersCounter;
    private final Counter internalTransfersCounter;
    private final Counter failedTransactionsCounter;
    private final Timer transactionDurationTimer;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              PixKeyRepository pixKeyRepository,
                              MeterRegistry meterRegistry) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.pixKeyRepository = pixKeyRepository;

        // Inicialização de métricas registradas no Prometheus
        this.pixTransfersCounter = Counter.builder("map.bank.pix.transfers.total")
                .description("Total de transações PIX liquidadas com sucesso")
                .register(meterRegistry);

        this.internalTransfersCounter = Counter.builder("map.bank.internal.transfers.total")
                .description("Total de transferências internas liquidadas com sucesso")
                .register(meterRegistry);

        this.failedTransactionsCounter = Counter.builder("map.bank.transactions.failed.total")
                .description("Total de transações rejeitadas por falha de saldo ou regras de negócio")
                .register(meterRegistry);

        this.transactionDurationTimer = Timer.builder("map.bank.transaction.duration")
                .description("Tempo de latência para processamento completo da transação financeira")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    /**
     * Efetua um depósito financeiro em conta bancária.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse processDeposit(Long accountId, DepositRequest request) {
        return transactionDurationTimer.record(() -> {
            log.info("Processando depósito de R$ {} para a conta ID: {}", request.amount(), accountId);

            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada para o ID: " + accountId));

            if (account.getStatus() != AccountStatus.ACTIVE) {
                failedTransactionsCounter.increment();
                throw new BusinessException("Não é permitido efetuar depósito em conta inativa ou bloqueada.");
            }

            account.credit(request.amount());
            accountRepository.save(account);

            String txCode = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String desc = request.description() != null ? request.description() : "Depósito em conta";

            Transaction tx = new Transaction(
                    txCode,
                    null,
                    account,
                    request.amount(),
                    TransactionType.DEPOSIT,
                    TransactionStatus.COMPLETED,
                    desc
            );

            Transaction saved = transactionRepository.save(tx);
            log.info("Depósito efetivado. TX Code: {}", saved.getTransactionCode());
            return TransactionResponse.fromEntity(saved);
        });
    }

    /**
     * Efetua um saque financeiro com validação de saldo e cheque especial.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse processWithdrawal(Long accountId, WithdrawRequest request) {
        return transactionDurationTimer.record(() -> {
            log.info("Processando saque de R$ {} na conta ID: {}", request.amount(), accountId);

            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada para o ID: " + accountId));

            if (account.getStatus() != AccountStatus.ACTIVE) {
                failedTransactionsCounter.increment();
                throw new BusinessException("Conta bancária inativa ou bloqueada para saques.");
            }

            if (!account.hasAvailableBalance(request.amount())) {
                failedTransactionsCounter.increment();
                throw new InsufficientBalanceException(String.format(
                        "Saldo insuficiente para saque de R$ %.2f. Saldo disponível: R$ %.2f",
                        request.amount(), account.getTotalAvailableFunds()));
            }

            account.debit(request.amount());
            accountRepository.save(account);

            String txCode = "SAQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String desc = request.description() != null ? request.description() : "Saque em espécie";

            Transaction tx = new Transaction(
                    txCode,
                    account,
                    null,
                    request.amount(),
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.COMPLETED,
                    desc
            );

            Transaction saved = transactionRepository.save(tx);
            log.info("Saque efetivado. TX Code: {}", saved.getTransactionCode());
            return TransactionResponse.fromEntity(saved);
        });
    }

    /**
     * Efetua uma transferência entre contas correntes do MAP-Bank com suporte a chave de idempotência.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse processInternalTransfer(InternalTransferRequest request, String idempotencyKey) {
        return transactionDurationTimer.record(() -> {
            log.info("Processando transferência interna de {} para {} no valor de R$ {}",
                    request.sourceAccountNumber(), request.targetAccountNumber(), request.amount());

            // Verificação de idempotência bancária
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                var existingTx = transactionRepository.findByTransactionCode(idempotencyKey);
                if (existingTx.isPresent()) {
                    log.info("Transação idempotente já processada anteriormente: {}", idempotencyKey);
                    return TransactionResponse.fromEntity(existingTx.get());
                }
            }

            if (request.sourceAccountNumber().equalsIgnoreCase(request.targetAccountNumber())) {
                failedTransactionsCounter.increment();
                throw new BusinessException("A conta de origem e destino não podem ser idênticas.");
            }

            Account source = accountRepository.findByAccountNumber(request.sourceAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Conta de origem não localizada: " + request.sourceAccountNumber()));

            Account target = accountRepository.findByAccountNumber(request.targetAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Conta de destino não localizada: " + request.targetAccountNumber()));

            validateActiveAccounts(source, target);

            if (!source.hasAvailableBalance(request.amount())) {
                failedTransactionsCounter.increment();
                throw new InsufficientBalanceException(String.format(
                        "Saldo insuficiente na conta de origem (%s). Recursos disponíveis: R$ %.2f",
                        source.getAccountNumber(), source.getTotalAvailableFunds()));
            }

            // Débito e crédito atômicos
            source.debit(request.amount());
            target.credit(request.amount());

            accountRepository.save(source);
            accountRepository.save(target);

            String txCode = (idempotencyKey != null && !idempotencyKey.isBlank())
                    ? idempotencyKey
                    : "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Transaction tx = new Transaction(
                    txCode,
                    source,
                    target,
                    request.amount(),
                    TransactionType.INTERNAL_TRANSFER,
                    TransactionStatus.COMPLETED,
                    request.description() != null ? request.description() : "Transferência entre contas MAP-Bank"
            );

            Transaction saved = transactionRepository.save(tx);
            internalTransfersCounter.increment();
            log.info("Transferência concluída com sucesso. Código: {}", saved.getTransactionCode());
            return TransactionResponse.fromEntity(saved);
        });
    }

    /**
     * Efetua um pagamento instantâneo via PIX utilizando chave de endereçamento cadastrada.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse processPixTransfer(PixPaymentRequest request, String idempotencyKey) {
        return transactionDurationTimer.record(() -> {
            log.info("Processando PIX da conta {} para a chave {}", request.sourceAccountNumber(), request.targetPixKey());

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                var existingTx = transactionRepository.findByTransactionCode(idempotencyKey);
                if (existingTx.isPresent()) {
                    log.info("PIX idempotente já liquidado: {}", idempotencyKey);
                    return TransactionResponse.fromEntity(existingTx.get());
                }
            }

            Account source = accountRepository.findByAccountNumber(request.sourceAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Conta pagadora não localizada: " + request.sourceAccountNumber()));

            PixKey pixKey = pixKeyRepository.findByKeyValue(request.targetPixKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Chave PIX de destino não encontrada no DICT: " + request.targetPixKey()));

            Account target = pixKey.getAccount();

            if (source.getId().equals(target.getId())) {
                failedTransactionsCounter.increment();
                throw new BusinessException("A conta de origem e destino da transferência PIX não podem ser iguais.");
            }

            validateActiveAccounts(source, target);

            if (!source.hasAvailableBalance(request.amount())) {
                failedTransactionsCounter.increment();
                throw new InsufficientBalanceException(String.format(
                        "Saldo insuficiente para realizar PIX de R$ %.2f. Saldo disponível: R$ %.2f",
                        request.amount(), source.getTotalAvailableFunds()));
            }

            source.debit(request.amount());
            target.credit(request.amount());

            accountRepository.save(source);
            accountRepository.save(target);

            String txCode = (idempotencyKey != null && !idempotencyKey.isBlank())
                    ? idempotencyKey
                    : "PIX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Transaction tx = new Transaction(
                    txCode,
                    source,
                    target,
                    request.amount(),
                    TransactionType.PIX,
                    TransactionStatus.COMPLETED,
                    request.description() != null ? request.description() : "Pagamento instantâneo PIX"
            );

            Transaction saved = transactionRepository.save(tx);
            pixTransfersCounter.increment();
            log.info("Pagamento PIX liquidado no SPI/BACEN. Código: {}", saved.getTransactionCode());
            return TransactionResponse.fromEntity(saved);
        });
    }

    /**
     * Consulta o extrato bancário detalhado de uma conta, com filtro opcional por período.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountStatement(Long accountId, Instant startDate, Instant endDate, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Conta bancária não localizada para o ID: " + accountId);
        }

        Page<Transaction> page;
        if (startDate != null && endDate != null) {
            page = transactionRepository.findAccountStatementByPeriod(accountId, startDate, endDate, pageable);
        } else {
            page = transactionRepository.findAccountStatement(accountId, pageable);
        }

        return page.map(TransactionResponse::fromEntity);
    }

    private void validateActiveAccounts(Account source, Account target) {
        if (source.getStatus() != AccountStatus.ACTIVE) {
            failedTransactionsCounter.increment();
            throw new BusinessException("Conta de origem está inativa ou bloqueada.");
        }
        if (target.getStatus() != AccountStatus.ACTIVE) {
            failedTransactionsCounter.increment();
            throw new BusinessException("Conta de destino está inativa ou bloqueada.");
        }
    }
}
