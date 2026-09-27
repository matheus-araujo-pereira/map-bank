package com.mapbank.domain.enums;

/**
 * Operações bancárias e financeiras transacionáveis no ecossistema do MAP-Bank.
 *
 * @author Matheus Araújo Pereira
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    INTERNAL_TRANSFER,
    PIX,
    REVERSAL
}
