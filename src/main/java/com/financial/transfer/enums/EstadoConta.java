package com.financial.transfer.enums;

/**
 * Estados possíveis de uma conta.
 * Transições válidas:
 *   ATIVA -> BLOQUEADA
 *   BLOQUEADA -> ATIVA
 *   ATIVA -> ENCERRADA
 *   BLOQUEADA -> ENCERRADA
 * ENCERRADA é terminal — não há saída.
 */
public enum EstadoConta {
    ATIVA,
    BLOQUEADA,
    ENCERRADA;

    public boolean podeEnviar() {
        return this == ATIVA;
    }

    public boolean podeReceber() {
        return this == ATIVA || this == BLOQUEADA;
    }

    public boolean podeTransicionarPara(EstadoConta destino) {
        return switch (this) {
            case ATIVA     -> destino == BLOQUEADA || destino == ENCERRADA;
            case BLOQUEADA -> destino == ATIVA     || destino == ENCERRADA;
            case ENCERRADA -> false; // terminal
        };
    }
}
