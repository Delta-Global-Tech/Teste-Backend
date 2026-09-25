package com.financial.transfer.enums;

public enum EstadoTransferencia {
    CRIADA,
    CONFIRMADA,
    FALHADA,
    ESTORNADA;

    public boolean podeTransicionarPara(EstadoTransferencia destino) {
        return switch (this) {
            case CRIADA     -> destino == CONFIRMADA || destino == FALHADA;
            case CONFIRMADA -> destino == ESTORNADA;
            case FALHADA    -> false;
            case ESTORNADA  -> false;
        };
    }
}
