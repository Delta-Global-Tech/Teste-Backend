package com.marlonmachado.desafiotecnico.dto;

import java.time.Instant;

public record MovementDTO(
        Long id,
        String tipo,
        Double valor,
        Double saldoApos,
        Instant criadoEm
) {
}
