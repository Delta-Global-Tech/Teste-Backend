package com.marlonmachado.desafiotecnico.exceptions;

import org.springframework.http.HttpStatus;

public class DatabaseException extends BusinessException {
    public DatabaseException(String message) {
        super(HttpStatus.BAD_REQUEST, "ERRO_BANCO_DADOS", message);
    }
}
