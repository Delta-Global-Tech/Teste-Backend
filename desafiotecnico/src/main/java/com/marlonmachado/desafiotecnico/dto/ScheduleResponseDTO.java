package com.marlonmachado.desafiotecnico.dto;

public record ScheduleResponseDTO(
        Long agendamentoId,
        String contaOrigem,
        String contaDestino,
        Double valor,
        String executarEm,
        String estado
) {
}
