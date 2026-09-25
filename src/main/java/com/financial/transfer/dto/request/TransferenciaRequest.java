package com.financial.transfer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferenciaRequest(

    @NotBlank(message = "O campo 'contaOrigem' é obrigatório")
    String contaOrigem,

    @NotBlank(message = "O campo 'contaDestino' é obrigatório")
    String contaDestino,

    @NotNull(message = "O campo 'valor' é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    BigDecimal valor
) {}
