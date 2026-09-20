package com.marlonmachado.desafiotecnico.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MovementDTO(
        Long id,
        String tipo,
        BigDecimal valor,
        BigDecimal saldoApos,
        Instant criadoEm
) {
}
