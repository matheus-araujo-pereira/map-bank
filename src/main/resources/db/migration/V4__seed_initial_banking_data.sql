-- ==============================================================================
-- V4: Carga inicial de dados bancários (Seeds para demonstração e testes)
-- Clientes, Contas, Chaves PIX e Transações Iniciais
-- ==============================================================================

-- 1. Clientes
INSERT INTO tb_clients (id, name, document, document_type, email, phone, status, created_at)
VALUES
(1, 'Matheus Araujo Pereira', '12345678901', 'PF', 'matheus@mapbank.com', '+5511999998888', 'ACTIVE', CURRENT_TIMESTAMP),
(2, 'NTT DATA Brasil Solucoes Tecnologicas', '11222333000199', 'PJ', 'contato@nttdata.com', '+551130001000', 'ACTIVE', CURRENT_TIMESTAMP),
(3, 'Gabriel Souza Santos', '98765432100', 'PF', 'gabriel.souza@mapbank.com', '+5511977776666', 'ACTIVE', CURRENT_TIMESTAMP);

-- Ajustar a sequence de tb_clients para novos cadastros
ALTER SEQUENCE tb_clients_id_seq RESTART WITH 10;

-- 2. Contas Bancárias
INSERT INTO tb_accounts (id, account_number, agency, account_type, balance, overdraft_limit, status, version, client_id, created_at)
VALUES
(1, '10001-9', '0001', 'CORRENTE', 15000.00, 3000.00, 'ACTIVE', 0, 1, CURRENT_TIMESTAMP),
(2, '20002-8', '0001', 'CORRENTE', 500000.00, 50000.00, 'ACTIVE', 0, 2, CURRENT_TIMESTAMP),
(3, '30003-7', '0001', 'POUPANCA', 2500.00, 0.00, 'ACTIVE', 0, 3, CURRENT_TIMESTAMP);

-- Ajustar a sequence de tb_accounts para novos cadastros
ALTER SEQUENCE tb_accounts_id_seq RESTART WITH 10;

-- 3. Chaves PIX
INSERT INTO tb_pix_keys (id, key_value, key_type, account_id, status, created_at)
VALUES
(1, 'matheus@mapbank.com', 'EMAIL', 1, 'ACTIVE', CURRENT_TIMESTAMP),
(2, '12345678901', 'CPF', 1, 'ACTIVE', CURRENT_TIMESTAMP),
(3, '11222333000199', 'CNPJ', 2, 'ACTIVE', CURRENT_TIMESTAMP),
(4, 'gabriel.souza@mapbank.com', 'EMAIL', 3, 'ACTIVE', CURRENT_TIMESTAMP);

-- Ajustar a sequence de tb_pix_keys
ALTER SEQUENCE tb_pix_keys_id_seq RESTART WITH 10;

-- 4. Transações Históricas
INSERT INTO tb_transactions (id, transaction_code, source_account_id, target_account_id, amount, transaction_type, status, description, created_at)
VALUES
(1, 'TX-INIT-001', NULL, 1, 15000.00, 'DEPOSIT', 'COMPLETED', 'Aporte inicial de abertura de conta - MAP', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(2, 'TX-INIT-002', NULL, 2, 500000.00, 'DEPOSIT', 'COMPLETED', 'Capital de giro corporativo NTT DATA', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(3, 'TX-INIT-003', NULL, 3, 2500.00, 'DEPOSIT', 'COMPLETED', 'Depósito em conta poupança', CURRENT_TIMESTAMP - INTERVAL '1 day');

-- Ajustar a sequence de tb_transactions
ALTER SEQUENCE tb_transactions_id_seq RESTART WITH 10;
