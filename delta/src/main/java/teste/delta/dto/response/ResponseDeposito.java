package teste.delta.dto.response;

import java.math.BigDecimal;

import teste.delta.model.Conta;

public record ResponseDeposito(
    String conta,
    BigDecimal valor,
    BigDecimal saldoApos
) {

    public static ResponseDeposito fromEntity(Conta conta, BigDecimal valor){
        if (conta == null) {
            return null;
        }
        return new ResponseDeposito(
            conta.getNumero(),
            valor,
            conta.getSaldoCentavos()
        );
    }
}
