package com.mapbank.service;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.enums.LoanStatus;
import com.mapbank.domain.enums.TransactionStatus;
import com.mapbank.domain.enums.TransactionType;
import com.mapbank.domain.model.Account;
import com.mapbank.domain.model.Loan;
import com.mapbank.domain.model.Transaction;
import com.mapbank.dto.request.LoanContractRequest;
import com.mapbank.dto.request.LoanPaymentRequest;
import com.mapbank.dto.request.LoanSimulationRequest;
import com.mapbank.dto.response.LoanResponse;
import com.mapbank.dto.response.LoanSimulationResponse;
import com.mapbank.exception.BusinessException;
import com.mapbank.exception.InsufficientBalanceException;
import com.mapbank.exception.ResourceNotFoundException;
import com.mapbank.repository.AccountRepository;
import com.mapbank.repository.LoanRepository;
import com.mapbank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de simulação, contratação e amortização de operações de crédito bancário do MAP-Bank.
 *
 * <p>Utiliza o <b>Sistema Francês de Amortização (Tabela Price)</b> para cálculo de parcelas fixas
 * e juros compostos em operações de financiamento.</p>
 *
 * @author Matheus Araujo Pereira
 */
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    // Taxa de juros padrão de 2.50% ao mês para operações de crédito pessoal
    private static final BigDecimal DEFAULT_MONTHLY_INTEREST_RATE = new BigDecimal("0.0250");

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public LoanService(LoanRepository loanRepository,
                       AccountRepository accountRepository,
                       TransactionRepository transactionRepository) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Simula uma proposta de empréstimo calculando o valor fixo da parcela e o custo total da operação.
     *
     * <p>Fórmula da Tabela Price: {@code PMT = P * [i * (1+i)^n] / [(1+i)^n - 1]}</p>
     *
     * @param request parâmetros solicitados (valor e parcelas).
     * @return {@link LoanSimulationResponse} com a projeção financeira.
     */
    public LoanSimulationResponse simulateLoan(LoanSimulationRequest request) {
        log.info("Simulando empréstimo de R$ {} em {} parcelas", request.requestedAmount(), request.installments());

        BigDecimal p = request.requestedAmount();
        int n = request.installments();
        BigDecimal i = DEFAULT_MONTHLY_INTEREST_RATE;

        BigDecimal installmentAmount;
        if (n == 1) {
            installmentAmount = p.multiply(BigDecimal.ONE.add(i)).setScale(2, RoundingMode.HALF_UP);
        } else {
            double rate = i.doubleValue();
            double factor = Math.pow(1.0 + rate, n);
            double pmt = p.doubleValue() * (rate * factor) / (factor - 1.0);
            installmentAmount = BigDecimal.valueOf(pmt).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = installmentAmount.multiply(BigDecimal.valueOf(n)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalAmount.subtract(p).setScale(2, RoundingMode.HALF_UP);

        return new LoanSimulationResponse(
                p,
                i,
                "2.50%",
                n,
                installmentAmount,
                totalAmount,
                totalInterest
        );
    }

    /**
     * Formaliza e contrata o empréstimo, creditando os recursos imediatamente na conta do cliente.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoanResponse contractLoan(LoanContractRequest request) {
        log.info("Contratando empréstimo de R$ {} para a conta {}", request.requestedAmount(), request.accountNumber());

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não localizada: " + request.accountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Apenas contas bancárias ativas podem contratar empréstimos.");
        }

        var sim = simulateLoan(new LoanSimulationRequest(request.requestedAmount(), request.installments()));
        String loanCode = "LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Loan loan = new Loan(
                loanCode,
                account,
                sim.requestedAmount(),
                sim.interestRateMonthly(),
                sim.installments(),
                sim.installmentAmount(),
                sim.totalAmount()
        );

        Loan savedLoan = loanRepository.save(loan);

        // Liberação imediata dos recursos no saldo da conta corrente
        account.credit(savedLoan.getRequestedAmount());
        accountRepository.save(account);

        // Registro da liberação do crédito no Ledger de transações
        Transaction tx = new Transaction(
                "CRED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                null,
                account,
                savedLoan.getRequestedAmount(),
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "Liberação de crédito - Contrato: " + loanCode
        );
        transactionRepository.save(tx);

        log.info("Empréstimo {} contratado com sucesso. Recursos creditados na conta {}", loanCode, account.getAccountNumber());
        return LoanResponse.fromEntity(savedLoan);
    }

    /**
     * Efetua o pagamento / amortização de parcelas do empréstimo debitando da conta bancária.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LoanResponse amortizeLoan(Long loanId, LoanPaymentRequest request) {
        log.info("Amortizando R$ {} no contrato ID: {}", request.amount(), loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato de empréstimo não localizado para o ID: " + loanId));

        if (loan.getStatus() == LoanStatus.PAID_OFF) {
            throw new BusinessException("Este contrato de empréstimo já se encontra totalmente quitado.");
        }

        Account account = loan.getAccount();
        if (!account.hasAvailableBalance(request.amount())) {
            throw new InsufficientBalanceException("Saldo insuficiente na conta para efetuar o pagamento da parcela.");
        }

        // Débito do valor da conta
        account.debit(request.amount());
        accountRepository.save(account);

        // Amortização do saldo devedor
        loan.amortize(request.amount());
        Loan updatedLoan = loanRepository.save(loan);

        // Registro contábil da amortização
        Transaction tx = new Transaction(
                "PAG-LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                account,
                null,
                request.amount(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.COMPLETED,
                "Amortização de empréstimo - Contrato: " + loan.getLoanCode()
        );
        transactionRepository.save(tx);

        log.info("Amortização processada com sucesso no contrato {}. Saldo devedor: R$ {}",
                updatedLoan.getLoanCode(), updatedLoan.getRemainingBalance());
        return LoanResponse.fromEntity(updatedLoan);
    }

    /**
     * Lista os contratos de empréstimo de uma determinada conta bancária.
     */
    @Transactional(readOnly = true)
    public List<LoanResponse> listByAccount(Long accountId) {
        return loanRepository.findByAccountId(accountId).stream()
                .map(LoanResponse::fromEntity)
                .toList();
    }

    /**
     * Localiza um contrato de empréstimo por seu ID.
     */
    @Transactional(readOnly = true)
    public LoanResponse findById(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato de empréstimo não localizado para o ID: " + id));
        return LoanResponse.fromEntity(loan);
    }
}
