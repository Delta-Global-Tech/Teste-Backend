package teste.delta.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import teste.delta.model.Conta;
import teste.delta.model.Transferencia;

public record ResponseComprovanteTransferencia(
    UUID transferenciaId,
    String contaOrigem,
    String contaDestino,
    BigDecimal valor,
    int taxa,
    BigDecimal totalDebitado,
    BigDecimal saldoOrigemApos,
    OffsetDateTime concluidaEm
) {

    public static ResponseComprovanteTransferencia fromEntity(
        Transferencia transferencia,
        Conta contaOrigem,
        Conta contaDestino
    ) {
        if (transferencia == null) {
            return null;
        }
        return new ResponseComprovanteTransferencia(
            transferencia.getId(),
            contaOrigem.getNumero(),
            contaDestino.getNumero(),
            transferencia.getValorCentavos(),
            transferencia.getTaxaCentavos(),
            transferencia.getValorCentavos().add(BigDecimal.valueOf(transferencia.getTaxaCentavos())),
            contaOrigem.getSaldoCentavos(),
            transferencia.getConcluidaEm()
        );
    }

}
