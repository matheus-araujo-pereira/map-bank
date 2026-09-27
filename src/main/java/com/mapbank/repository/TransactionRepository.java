package com.mapbank.repository;

import com.mapbank.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para auditoria contábil e extratos de transações financeiras.
 *
 * @author Matheus Araujo Pereira
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionCode(String transactionCode);

    boolean existsByTransactionCode(String transactionCode);

    /**
     * Consulta paginada do extrato bancário de uma conta (transações de entrada ou saída).
     */
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findAccountStatement(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Extrato filtrado por período de datas (Data Inicial e Data Final).
     */
    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId) " +
           "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findAccountStatementByPeriod(
            @Param("accountId") Long accountId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );
}
