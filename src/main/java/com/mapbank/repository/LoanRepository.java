package com.mapbank.repository;

import com.mapbank.domain.enums.LoanStatus;
import com.mapbank.domain.model.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para contratos de crédito e financiamentos bancários.
 *
 * @author Matheus Araujo Pereira
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanCode(String loanCode);

    List<Loan> findByAccountId(Long accountId);

    Page<Loan> findAllByStatus(LoanStatus status, Pageable pageable);
}
