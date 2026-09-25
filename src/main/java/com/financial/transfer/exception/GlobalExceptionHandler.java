package com.financial.transfer.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Erros de negócio controlados pela aplicação. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest req) {
        log.warn("ApiException [{}] {}: {}", ex.getStatus().value(), ex.getCodigo(), ex.getMessage());
        return build(ex.getStatus(), ex.getCodigo(), ex.getMessage(), req.getRequestURI());
    }

    /** Bean Validation: @Valid falhou. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, ErrorCode.CAMPO_INVALIDO, mensagem, req.getRequestURI());
    }

    /** Header Idempotency-Key ausente. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.HEADER_AUSENTE,
            "Header obrigatório ausente: " + ex.getHeaderName(), req.getRequestURI());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        log.warn("Optimistic locking falhou: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLITO_CONCORRENCIA,
            "Conflito de concorrência. Tente novamente.", req.getRequestURI());
    }

    /** Catch-all para erros inesperados. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.ERRO_INTERNO,
            "Erro interno do servidor", req.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String codigo, String mensagem, String path) {
        ErrorResponse body = new ErrorResponse(
            OffsetDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            codigo,
            mensagem,
            path
        );
        return ResponseEntity.status(status).body(body);
    }

    /** Corpo padrão de erro. */
    public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String codigo,
        String message,
        String path
    ) {}
}
