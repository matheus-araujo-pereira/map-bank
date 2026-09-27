-- ==============================================================================
-- V2: Criação das tabelas de Chaves PIX e Histórico de Transações Bancárias
-- Índices otimizados para busca de extratos por data e chave PIX
-- ==============================================================================

CREATE TABLE tb_pix_keys (
    id BIGSERIAL PRIMARY KEY,
    key_value VARCHAR(100) NOT NULL UNIQUE,
    key_type VARCHAR(20) NOT NULL,
    account_id BIGINT NOT NULL REFERENCES tb_accounts(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pix_keys_value ON tb_pix_keys(key_value);
CREATE INDEX idx_pix_keys_account_id ON tb_pix_keys(account_id);

CREATE TABLE tb_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_code VARCHAR(50) NOT NULL UNIQUE,
    source_account_id BIGINT REFERENCES tb_accounts(id),
    target_account_id BIGINT REFERENCES tb_accounts(id),
    amount NUMERIC(15, 2) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_code ON tb_transactions(transaction_code);
CREATE INDEX idx_transactions_source ON tb_transactions(source_account_id);
CREATE INDEX idx_transactions_target ON tb_transactions(target_account_id);
CREATE INDEX idx_transactions_created_at ON tb_transactions(created_at);
CREATE INDEX idx_transactions_account_date ON tb_transactions(source_account_id, created_at DESC);
