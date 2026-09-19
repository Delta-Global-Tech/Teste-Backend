package com.marlonmachado.desafiotecnico.dto;

public record SummaryDTO(
        Long contasAtivasAgora,
        Double saldoTotalUsuariosAgora,
        Long agendamentosPendentesAgora,
        Long transferenciasNaJanela,
        Double valorTransferidoNaJanela,
        Double taxasNaJanela,
        Double ticketMedioNaJanela,
        Long estornosNaJanela,
        Boolean somaDosMovimentosEhZero
) {
}
