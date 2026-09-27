package com.mapbank.domain.model;

import com.mapbank.domain.enums.AccountStatus;
import com.mapbank.domain.enums.AccountType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade central representando uma conta bancária no MAP-Bank.
 *
 * <p>Implementa controle de concorrência com <b>Optimistic Locking</b> via a anotação
 * {@link Version} na coluna {@code version}, garantindo que operações concorrentes de débito
 * e crédito não gerem anomalias de <i>lost update</i> ou inconsistências de saldo.</p>
 *
 * @author Matheus Araujo Pereira
 */
@Entity
@Table(name = "tb_accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 10)
    private String agency = "0001";

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "overdraft_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * Controle de concorrência otimista (Optimistic Locking).
     * O Hibernate incrementa este campo automaticamente a cada UPDATE.
     * Caso outra transação tenha alterado a conta antes, lança {@link OptimisticLockException}.
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PixKey> pixKeys = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Account() {
    }

    public Account(String accountNumber, String agency, AccountType accountType, BigDecimal initialBalance, BigDecimal overdraftLimit, Client client) {
        this.accountNumber = accountNumber;
        this.agency = agency;
        this.accountType = accountType;
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.overdraftLimit = overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO;
        this.client = client;
        this.status = AccountStatus.ACTIVE;
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
     * Retorna o total de recursos imediatamente disponíveis na conta (Saldo Real + Cheque Especial).
     *
     * @return {@link BigDecimal} com o total disponível.
     */
    public BigDecimal getTotalAvailableFunds() {
        return this.balance.add(this.overdraftLimit);
    }

    /**
     * Verifica se a conta possui fundos suficientes para a operação solicitada.
     *
     * @param amount valor a ser debitado.
     * @return {@code true} se houver saldo + cheque especial suficiente.
     */
    public boolean hasAvailableBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return getTotalAvailableFunds().compareTo(amount) >= 0;
    }

    /**
     * Efetua o débito do valor no saldo da conta.
     *
     * @param amount valor a debitar.
     */
    public void debit(BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Saldo insuficiente para efetivar o débito de R$ " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Efetua o crédito do valor no saldo da conta.
     *
     * @param amount valor a creditar.
     */
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor creditado deve ser estritamente positivo");
        }
        this.balance = this.balance.add(amount);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<PixKey> getPixKeys() {
        return pixKeys;
    }

    public void setPixKeys(List<PixKey> pixKeys) {
        this.pixKeys = pixKeys;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
