package teste.delta.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro de negócio com status HTTP e um código estável, consumido pelo cliente da API.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String message){
        super(message);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
