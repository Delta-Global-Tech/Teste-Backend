package com.financial.transfer.dto.response;

import com.financial.transfer.enums.EstadoAgendamento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AgendamentoResponse(
    Long agendamentoId,
    String contaOrigem,
    String contaDestino,
    BigDecimal valor,
    OffsetDateTime executarEm,
    EstadoAgendamento estado,
    OffsetDateTime criadoEm
) {}
