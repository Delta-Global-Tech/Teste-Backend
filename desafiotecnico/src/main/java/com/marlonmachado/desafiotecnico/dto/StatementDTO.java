package com.marlonmachado.desafiotecnico.dto;

import java.math.BigDecimal;
import java.util.List;

public record StatementDTO(
        String numeroConta,
        BigDecimal saldoAtual,
        List<MovementDTO> movimentos
) {
}
