-- ==============================================================================
-- V1: Criação das tabelas centrais de Clientes e Contas Bancárias do MAP-Bank
-- Inclui suporte a Optimistic Locking (coluna version) e índices de performance
-- ==============================================================================

CREATE TABLE tb_clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    document VARCHAR(20) NOT NULL UNIQUE,
    document_type VARCHAR(10) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(25) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_clients_document ON tb_clients(document);
CREATE INDEX idx_clients_email ON tb_clients(email);
CREATE INDEX idx_clients_status ON tb_clients(status);

CREATE TABLE tb_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    agency VARCHAR(10) NOT NULL DEFAULT '0001',
    account_type VARCHAR(20) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    overdraft_limit NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    client_id BIGINT NOT NULL REFERENCES tb_clients(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_accounts_number ON tb_accounts(account_number);
CREATE INDEX idx_accounts_client_id ON tb_accounts(client_id);
CREATE INDEX idx_accounts_status ON tb_accounts(status);
