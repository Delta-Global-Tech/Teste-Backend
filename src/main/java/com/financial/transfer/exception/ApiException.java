package com.financial.transfer.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base da aplicação. Carrega o status HTTP e o código de erro estável.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo()     { return codigo; }

    // --- Fábrica de exceções comuns ---

    public static ApiException contaNaoEncontrada(String numero) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CONTA_NAO_ENCONTRADA,
            "Conta não encontrada: " + numero);
    }

    public static ApiException transferenciaNaoEncontrada(Long id) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.TRANSFERENCIA_NAO_ENCONTRADA,
            "Transferência não encontrada: " + id);
    }

    public static ApiException contaBloqueada(String numero) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONTA_BLOQUEADA,
            "Conta bloqueada não pode enviar: " + numero);
    }

    public static ApiException contaEncerrada(String numero) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONTA_ENCERRADA,
            "Conta encerrada não pode operar: " + numero);
    }

    public static ApiException contaDestinoEncerrada(String numero) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONTA_DESTINO_ENCERRADA,
            "Conta destino encerrada não pode receber: " + numero);
    }

    public static ApiException contasIguais() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONTAS_IGUAIS,
            "Conta origem e destino não podem ser iguais");
    }

    public static ApiException saldoInsuficiente() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.SALDO_INSUFICIENTE,
            "Saldo insuficiente para realizar a transferência");
    }

    public static ApiException limiteDiarioExcedido() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.LIMITE_DIARIO_EXCEDIDO,
            "Limite diário de transferência excedido");
    }

    public static ApiException valorInvalido(String detalhe) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.VALOR_INVALIDO, detalhe);
    }

    public static ApiException transferenciaJaEstornada(Long id) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.TRANSFERENCIA_JA_ESTORNADA,
            "Transferência " + id + " já foi estornada");
    }

    public static ApiException transferenciaNaoConfirmada(Long id) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.TRANSFERENCIA_NAO_CONFIRMADA,
            "Somente transferências CONFIRMADAS podem ser estornadas. ID: " + id);
    }

    public static ApiException estornoSemSaldo() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ESTORNO_SEM_SALDO,
            "Conta destino original não possui saldo suficiente para o estorno");
    }

    public static ApiException dataPassado() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DATA_PASSADO,
            "A data de execução não pode estar no passado");
    }

    public static ApiException idempotenciaConflito() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCIA_CONFLITO,
            "Mesma Idempotency-Key com corpo diferente da requisição original");
    }

    public static ApiException conflitoConcorrencia() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLITO_CONCORRENCIA,
            "Conflito de concorrência. Tente novamente.");
    }
}
