package com.mapbank.exception;

/**
 * Exceção lançada quando uma entidade solicitada não é localizada no banco de dados.
 *
 * @author Matheus Araújo Pereira
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
