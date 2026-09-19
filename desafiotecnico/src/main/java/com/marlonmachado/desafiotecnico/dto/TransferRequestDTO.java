package com.marlonmachado.desafiotecnico.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferRequestDTO(
        @NotBlank(message = "Conta de origem é obrigatória")
        String contaOrigem,

        @NotBlank(message = "Conta de destino é obrigatória")
        String contaDestino,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        Double valor
) {
}
