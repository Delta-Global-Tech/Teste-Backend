package teste.delta.dto.requests;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestAgendamentoTransferencia(
    @NotBlank String contaOrigem,
    @NotBlank String contaDestino,
    @NotNull BigDecimal valor,
    @NotNull OffsetDateTime executarEm
) {

}
