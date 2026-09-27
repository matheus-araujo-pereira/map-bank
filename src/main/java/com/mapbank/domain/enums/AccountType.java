package com.mapbank.domain.enums;

/**
 * Define a tipologia das contas bancárias suportadas pelo MAP-Bank.
 *
 * <ul>
 *   <li>{@link #CORRENTE}: Conta corrente padrão com suporte a cheque especial e PIX.</li>
 *   <li>{@link #POUPANCA}: Conta de investimento com rendimento programado.</li>
 *   <li>{@link #SALARIO}: Conta destinada exclusivamente ao recebimento de proventos salariais.</li>
 * </ul>
 *
 * @author Matheus Araujo Pereira
 */
public enum AccountType {
    CORRENTE,
    POUPANCA,
    SALARIO
}
