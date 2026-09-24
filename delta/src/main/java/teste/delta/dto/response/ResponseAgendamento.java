package teste.delta.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import teste.delta.model.Agendamento;
import teste.delta.model.Conta;
import teste.delta.model.enums.EstadoAgendamento;

public record ResponseAgendamento(
    UUID agendamentoId,
    String contaOrigem,
    String contaDestino,
    BigDecimal valor,
    OffsetDateTime executarEm,
    EstadoAgendamento estado
) {

    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    public static ResponseAgendamento fromEntity(Agendamento agendamento, Conta contaOrigem, Conta contaDestino){
        if (agendamento == null) {
            return null;
        }
        return new ResponseAgendamento(
            agendamento.getId(),
            contaOrigem.getNumero(),
            contaDestino.getNumero(),
            agendamento.getValorCentavos(),
            agendamento.getExecutarEm().atZoneSameInstant(FUSO_BRASIL).toOffsetDateTime(),
            agendamento.getEstado()
        );
    }
}
