package com.marlonmachado.desafiotecnico.controllers.handlers;

import com.marlonmachado.desafiotecnico.dto.CustomError;
import com.marlonmachado.desafiotecnico.dto.ValidationError;
import com.marlonmachado.desafiotecnico.exceptions.BusinessException;
import com.marlonmachado.desafiotecnico.exceptions.DatabaseException;
import com.marlonmachado.desafiotecnico.exceptions.IllegalStateTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CustomError> businessException(BusinessException e, HttpServletRequest request) {
        HttpStatus status = e.getStatus();
        CustomError err = new CustomError(Instant.now(), status.value(), status.getReasonPhrase(),
                e.getCodigo(), e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomError> methodArgumentNotValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        ValidationError err = new ValidationError(Instant.now(), status.value(),status.getReasonPhrase(),"Dados invalidos" ,  "Um ou mais campos estão inválidos", request.getRequestURI());

        for(FieldError f : e.getBindingResult().getFieldErrors() ){
            err.addError(f.getField(), f.getDefaultMessage());
        }

        return ResponseEntity.status(status).body(err);
    }

}
