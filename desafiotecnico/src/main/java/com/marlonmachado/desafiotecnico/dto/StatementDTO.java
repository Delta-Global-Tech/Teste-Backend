package com.marlonmachado.desafiotecnico.dto;

import java.util.List;

public record StatementDTO(
        String numeroConta,
        Double saldoAtual,
        List<MovementDTO> movimentos
) {
}
