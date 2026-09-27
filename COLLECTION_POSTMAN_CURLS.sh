#!/usr/bin/env bash
# ==============================================================================
# MAP-BANK (Matheus Araújo Pereira Bank) - Script Executável de Testes da API
# Vaga: Desenvolvedor Java Pleno - NTT DATA
# ==============================================================================

BASE_URL="http://localhost:8080"
echo "======================================================================"
echo "🏦 TESTANDO ENDPOINTS DO MAP-BANK: $BASE_URL"
echo "======================================================================"

echo -e "\n1. OBSERVABILIDADE & HEALTH CHECK (Spring Boot Actuator)"
curl -s -X GET "$BASE_URL/actuator/health" | jq .

echo -e "\n2. MÉTRICAS EXPOSTAS NO PROMETHEUS (Micrometer)"
curl -s -X GET "$BASE_URL/actuator/prometheus" | grep -E "map_bank" | head -n 15

echo -e "\n3. LISTAR CLIENTES (Seed inicial de dados)"
curl -s -X GET "$BASE_URL/api/v1/clients" | jq .

echo -e "\n4. CADASTRAR NOVO CLIENTE TITULAR (Pessoa Física)"
CLIENT_ID=$(curl -s -X POST "$BASE_URL/api/v1/clients" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Carlos Eduardo Lima",
    "document": "77788899900",
    "documentType": "PF",
    "email": "carlos.lima@mapbank.com",
    "phone": "+5511987654321"
  }' | jq -r .id)
echo "Cliente criado com ID: $CLIENT_ID"

echo -e "\n5. ABRIR CONTA CORRENTE PARA O NOVO CLIENTE"
ACCOUNT_JSON=$(curl -s -X POST "$BASE_URL/api/v1/accounts" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": $CLIENT_ID,
    \"accountType\": \"CORRENTE\",
    \"initialDeposit\": 1500.00,
    \"overdraftLimit\": 2000.00
  }")
echo "$ACCOUNT_JSON" | jq .
ACCOUNT_NUM=$(echo "$ACCOUNT_JSON" | jq -r .accountNumber)
ACCOUNT_ID=$(echo "$ACCOUNT_JSON" | jq -r .id)

echo -e "\n6. CADASTRAR CHAVE PIX NA NOVA CONTA"
curl -s -X POST "$BASE_URL/api/v1/pix/accounts/$ACCOUNT_ID/keys" \
  -H "Content-Type: application/json" \
  -d '{
    "keyType": "EMAIL",
    "keyValue": "carlos.lima@mapbank.com"
  }' | jq .

echo -e "\n7. CONSULTAR CHAVE PIX NO DICT"
curl -s -X GET "$BASE_URL/api/v1/pix/keys/carlos.lima@mapbank.com" | jq .

echo -e "\n8. EFETUAR DEPÓSITO NA CONTA"
curl -s -X POST "$BASE_URL/api/v1/transactions/accounts/$ACCOUNT_ID/deposit" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00,
    "description": "Depósito de bonificação de boas-vindas"
  }' | jq .

echo -e "\n9. EFETUAR SAQUE NA CONTA"
curl -s -X POST "$BASE_URL/api/v1/transactions/accounts/$ACCOUNT_ID/withdraw" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 200.00,
    "description": "Saque terminal caixa eletrônico"
  }' | jq .

echo -e "\n10. TRANSFERÊNCIA INTERNA COM CHAVE DE IDEMPOTÊNCIA"
curl -s -X POST "$BASE_URL/api/v1/transactions/transfer" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: IDEMPOTENT-DEMO-001" \
  -d "{
    \"sourceAccountNumber\": \"$ACCOUNT_NUM\",
    \"targetAccountNumber\": \"10001-9\",
    \"amount\": 300.00,
    \"description\": \"Transferência entre contas MAP-Bank\"
  }" | jq .

echo -e "\n11. TRANSFERÊNCIA PIX PARA CHAVE DO MATHEUS"
curl -s -X POST "$BASE_URL/api/v1/transactions/pix" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: PIX-IDEMPOTENT-DEMO-001" \
  -d "{
    \"sourceAccountNumber\": \"$ACCOUNT_NUM\",
    \"targetPixKey\": \"matheus@mapbank.com\",
    \"amount\": 150.00,
    \"description\": \"Pagamento PIX de consultoria\"
  }" | jq .

echo -e "\n12. SIMULAÇÃO DE EMPRÉSTIMO (TABELA PRICE)"
curl -s -X POST "$BASE_URL/api/v1/loans/simulate" \
  -H "Content-Type: application/json" \
  -d '{
    "requestedAmount": 20000.00,
    "installments": 24
  }' | jq .

echo -e "\n13. CONTRATAÇÃO DE EMPRÉSTIMO COM CRÉDITO AUTOMÁTICO EM CONTA"
LOAN_JSON=$(curl -s -X POST "$BASE_URL/api/v1/loans/contract" \
  -H "Content-Type: application/json" \
  -d "{
    \"accountNumber\": \"$ACCOUNT_NUM\",
    \"requestedAmount\": 5000.00,
    \"installments\": 12
  }")
echo "$LOAN_JSON" | jq .
LOAN_ID=$(echo "$LOAN_JSON" | jq -r .id)

echo -e "\n14. AMORTIZAÇÃO DE PARCELA DO EMPRÉSTIMO"
curl -s -X POST "$BASE_URL/api/v1/loans/$LOAN_ID/amortize" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 487.89
  }' | jq .

echo -e "\n15. CONSULTAR EXTRATO BANCÁRIO COMPLETO DA CONTA"
curl -s -X GET "$BASE_URL/api/v1/transactions/accounts/$ACCOUNT_ID/statement" | jq .

echo -e "\n======================================================================"
echo "✅ BATERIA DE TESTES DE ENDPOINTS CONCLUÍDA COM SUCESSO!"
echo "======================================================================"
