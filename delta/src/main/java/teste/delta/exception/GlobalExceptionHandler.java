package teste.delta.exception;

import java.time.OffsetDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import teste.delta.dto.response.ResponseError;

/**
 * Converte as exceções de negócio no corpo de erro padrão da API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ResponseError> handleApiException(ApiException ex, HttpServletRequest request){
        ResponseError body = new ResponseError(
            OffsetDateTime.now(),
            ex.getStatus().value(),
            ex.getMessage(),
            ex.getCodigo(),
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
