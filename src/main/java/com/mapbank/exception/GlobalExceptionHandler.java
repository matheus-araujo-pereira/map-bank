package com.mapbank.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tratamento global de exceções da aplicação bancária aderente ao padrão <b>RFC 7807 (Problem Details)</b>,
 * nativo do Spring Boot 3 / Spring Framework 6.
 *
 * <p>Centraliza o mapeamento de falhas de negócio, erros de validação e concorrência financeira,
 * gerando respostas padronizadas para clientes e microsserviços integrados.</p>
 *
 * @author Matheus Araujo Pereira
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("https://mapbank.com/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Tentativa de débito com saldo insuficiente: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Saldo Insuficiente");
        problem.setType(URI.create("https://mapbank.com/errors/insufficient-balance"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        log.warn("Violação de regra de negócio: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Violação de Regra de Negócio");
        problem.setType(URI.create("https://mapbank.com/errors/business-rule"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Tratamento de falha de concorrência financeira (Optimistic Locking).
     * Ocorre quando duas requisições tentam alterar o saldo da mesma conta simultaneamente.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.error("Conflito de concorrência detectado em conta bancária (Optimistic Lock): {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "A conta bancária foi modificada concorrentemente por outra transação. Por favor, reenvie a operação."
        );
        problem.setTitle("Conflito de Concorrência Bancária");
        problem.setType(URI.create("https://mapbank.com/errors/concurrency-conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erros de validação nos campos informados.");
        problem.setTitle("Dados de Requisição Inválidos");
        problem.setType(URI.create("https://mapbank.com/errors/validation-failed"));
        problem.setProperty("invalidFields", fieldErrors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Erro interno inesperado no servidor: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno no servidor bancário. Nossa equipe de engenharia foi notificada."
        );
        problem.setTitle("Erro Interno do Servidor");
        problem.setType(URI.create("https://mapbank.com/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
