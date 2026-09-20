package com.marlonmachado.desafiotecnico.dto;

import java.math.BigDecimal;

public record SummaryDTO(
        Long contasAtivasAgora,
        BigDecimal saldoTotalUsuariosAgora,
        Long agendamentosPendentesAgora,
        Long transferenciasNaJanela,
        BigDecimal valorTransferidoNaJanela,
        BigDecimal taxasNaJanela,
        BigDecimal ticketMedioNaJanela,
        Long estornosNaJanela,
        Boolean somaDosMovimentosEhZero
) {
}
