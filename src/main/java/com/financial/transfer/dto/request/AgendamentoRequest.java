package com.financial.transfer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AgendamentoRequest(

    @NotBlank(message = "O campo 'contaOrigem' é obrigatório")
    String contaOrigem,

    @NotBlank(message = "O campo 'contaDestino' é obrigatório")
    String contaDestino,

    @NotNull(message = "O campo 'valor' é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    BigDecimal valor,

    @NotNull(message = "O campo 'executarEm' é obrigatório")
    @Future(message = "A data de execução não pode estar no passado")
    OffsetDateTime executarEm
) {}
