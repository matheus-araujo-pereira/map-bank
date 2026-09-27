package com.mapbank.repository;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para gerenciamento e persistência de contas bancárias.
 *
 * @author Matheus Araújo Pereira
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByClientId(Long clientId);

    Page<Account> findAllByStatus(AccountStatus status, Pageable pageable);
}
