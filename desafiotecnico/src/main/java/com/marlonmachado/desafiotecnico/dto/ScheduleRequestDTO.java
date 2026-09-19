package com.marlonmachado.desafiotecnico.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScheduleRequestDTO(
        @NotBlank(message = "Conta de origem é obrigatória")
        String contaOrigem,

        @NotBlank(message = "Conta de destino é obrigatória")
        String contaDestino,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        Double valor,

        @NotNull(message = "Data de execução é obrigatória")
        Instant executarEm
) {
}
