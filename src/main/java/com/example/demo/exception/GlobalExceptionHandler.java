package com.example.demo.exception;

import com.example.demo.dto.erro.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> validationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        FieldError fieldError = exception.getBindingResult().getFieldErrors().getFirst();

        String codigo = "VALIDACAO_INVALIDA";
        String message = fieldError.getDefaultMessage();

        ErrorResponseDTO resposta = new ErrorResponseDTO(
                OffsetDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                codigo,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resposta);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> illegalArgumentError(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        ErrorResponseDTO resposta = new ErrorResponseDTO(
                OffsetDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                "REGRA_NEGOCIO",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resposta);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> illegalStateError(
            IllegalStateException exception,
            HttpServletRequest request
    ) {

        ErrorResponseDTO resposta = new ErrorResponseDTO(
                OffsetDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "OPERACAO_INVALIDA",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(resposta);
    }
}