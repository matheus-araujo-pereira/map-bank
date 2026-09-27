-- ==============================================================================
-- V3: Criação da tabela de Operações de Crédito e Empréstimos Bancários
-- ==============================================================================

CREATE TABLE tb_loans (
    id BIGSERIAL PRIMARY KEY,
    loan_code VARCHAR(50) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL REFERENCES tb_accounts(id),
    requested_amount NUMERIC(15, 2) NOT NULL,
    interest_rate_monthly NUMERIC(6, 4) NOT NULL,
    installments INTEGER NOT NULL,
    installment_amount NUMERIC(15, 2) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    remaining_balance NUMERIC(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_loans_code ON tb_loans(loan_code);
CREATE INDEX idx_loans_account_id ON tb_loans(account_id);
CREATE INDEX idx_loans_status ON tb_loans(status);
