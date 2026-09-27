package com.mapbank.exception;

/**
 * Exceção base de violação de regra de negócio bancária no MAP-Bank.
 *
 * @author Matheus Araujo Pereira
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
