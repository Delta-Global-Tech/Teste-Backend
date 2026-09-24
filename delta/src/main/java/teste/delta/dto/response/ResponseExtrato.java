package teste.delta.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import teste.delta.model.Conta;
import teste.delta.model.Movimento;
import teste.delta.model.enums.TipoMovimento;

public record ResponseExtrato(
    String conta,
    BigDecimal saldoAtual,
    int page,
    int size,
    long totalElementos,
    int totalPaginas,
    List<Item> movimentos
) {

    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    public record Item(
        Long sequencia,
        UUID transferenciaId,
        TipoMovimento tipo,
        BigDecimal valor,
        BigDecimal saldoApos,
        OffsetDateTime criadoEm
    ) {}

    public static ResponseExtrato fromEntity(Conta conta, Page<Movimento> movimentos){
        return new ResponseExtrato(
            conta.getNumero(),
            conta.getSaldoCentavos(),
            movimentos.getNumber(),
            movimentos.getSize(),
            movimentos.getTotalElements(),
            movimentos.getTotalPages(),
            movimentos.map(m -> new Item(
                m.getSequencia(),
                m.getTransferenciaId(),
                m.getTipo(),
                m.getValorCentavos(),
                m.getSaldoAposCentavos(),
                m.getCriadoEm().atZoneSameInstant(FUSO_BRASIL).toOffsetDateTime()
            )).getContent()
        );
    }
}
