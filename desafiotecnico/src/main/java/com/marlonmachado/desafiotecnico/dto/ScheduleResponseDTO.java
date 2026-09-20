package com.marlonmachado.desafiotecnico.dto;

import java.math.BigDecimal;

public record ScheduleResponseDTO(
        Long agendamentoId,
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        String executarEm,
        String estado
) {
}
