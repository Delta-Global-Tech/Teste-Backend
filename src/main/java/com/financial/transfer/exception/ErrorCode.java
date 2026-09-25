package com.financial.transfer.exception;

public final class ErrorCode {

    private ErrorCode() {}

    public static final String CAMPO_INVALIDO           = "CAMPO_INVALIDO";
    public static final String HEADER_AUSENTE           = "HEADER_AUSENTE";

    public static final String CONTA_NAO_ENCONTRADA     = "CONTA_NAO_ENCONTRADA";
    public static final String TRANSFERENCIA_NAO_ENCONTRADA = "TRANSFERENCIA_NAO_ENCONTRADA";

    public static final String CONTA_BLOQUEADA          = "CONTA_BLOQUEADA";
    public static final String CONTA_ENCERRADA          = "CONTA_ENCERRADA";
    public static final String CONTA_DESTINO_ENCERRADA  = "CONTA_DESTINO_ENCERRADA";
    public static final String CONTAS_IGUAIS            = "CONTAS_IGUAIS";
    public static final String TRANSICAO_INVALIDA       = "TRANSICAO_INVALIDA";

    public static final String SALDO_INSUFICIENTE       = "SALDO_INSUFICIENTE";
    public static final String LIMITE_DIARIO_EXCEDIDO   = "LIMITE_DIARIO_EXCEDIDO";
    public static final String VALOR_INVALIDO           = "VALOR_INVALIDO";

    public static final String TRANSFERENCIA_NAO_CONFIRMADA = "TRANSFERENCIA_NAO_CONFIRMADA";
    public static final String TRANSFERENCIA_JA_ESTORNADA   = "TRANSFERENCIA_JA_ESTORNADA";
    public static final String ESTORNO_SEM_SALDO             = "ESTORNO_SEM_SALDO";

    // Agendamento
    public static final String DATA_PASSADO             = "DATA_PASSADO";

    public static final String IDEMPOTENCIA_CONFLITO    = "IDEMPOTENCIA_CONFLITO";

    public static final String CONFLITO_CONCORRENCIA    = "CONFLITO_CONCORRENCIA";

    public static final String ERRO_INTERNO             = "ERRO_INTERNO";
}
