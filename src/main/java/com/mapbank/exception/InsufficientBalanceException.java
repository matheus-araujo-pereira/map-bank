package com.mapbank.exception;

/**
 * Exceção de negócio lançada quando a conta não possui saldo ou limite suficiente para a operação.
 *
 * @author Matheus Araujo Pereira
 */
public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
