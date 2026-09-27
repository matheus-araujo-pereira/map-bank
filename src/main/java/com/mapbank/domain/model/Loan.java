package com.mapbank.domain.model;

import com.mapbank.domain.enums.LoanStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidade de operação de crédito e financiamento bancário do MAP-Bank.
 *
 * <p>Controla o valor do principal contratado, taxa de juros calculada, quantidade de parcelas,
 * saldo devedor remanescente e amortização.</p>
 *
 * @author Matheus Araujo Pereira
 */
@Entity
@Table(name = "tb_loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_code", nullable = false, unique = true, length = 50)
    private String loanCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "interest_rate_monthly", nullable = false, precision = 6, scale = 4)
    private BigDecimal interestRateMonthly;

    @Column(nullable = false)
    private Integer installments;

    @Column(name = "installment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "remaining_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.APPROVED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Loan() {
    }

    public Loan(String loanCode, Account account, BigDecimal requestedAmount, BigDecimal interestRateMonthly,
                Integer installments, BigDecimal installmentAmount, BigDecimal totalAmount) {
        this.loanCode = loanCode;
        this.account = account;
        this.requestedAmount = requestedAmount;
        this.interestRateMonthly = interestRateMonthly;
        this.installments = installments;
        this.installmentAmount = installmentAmount;
        this.totalAmount = totalAmount;
        this.remainingBalance = totalAmount;
        this.status = LoanStatus.APPROVED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Amortiza uma parcela ou valor sobre o saldo devedor.
     *
     * @param paymentAmount valor a amortizar.
     */
    public void amortize(BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor de amortização deve ser positivo");
        }
        this.remainingBalance = this.remainingBalance.subtract(paymentAmount);
        if (this.remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.remainingBalance = BigDecimal.ZERO;
            this.status = LoanStatus.PAID_OFF;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoanCode() {
        return loanCode;
    }

    public void setLoanCode(String loanCode) {
        this.loanCode = loanCode;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getInterestRateMonthly() {
        return interestRateMonthly;
    }

    public void setInterestRateMonthly(BigDecimal interestRateMonthly) {
        this.interestRateMonthly = interestRateMonthly;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    public BigDecimal getInstallmentAmount() {
        return installmentAmount;
    }

    public void setInstallmentAmount(BigDecimal installmentAmount) {
        this.installmentAmount = installmentAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
