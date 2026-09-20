package com.marlonmachado.desafiotecnico.exceptions;

import com.marlonmachado.desafiotecnico.entities.enums.TransferStatus;
import org.springframework.http.HttpStatus;

public class IllegalStateTransitionException extends BusinessException {
    public IllegalStateTransitionException(TransferStatus from, TransferStatus to) {
        super(HttpStatus.CONFLICT, "TRANSICAO_INVALIDA", "Transição de estado inválida: " + from + " -> " + to);
    }
}
