package com.financial.transfer.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositoRequest(

    @NotNull(message = "O campo 'valor' é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    @DecimalMax(value = "10000.00", message = "O valor máximo por depósito é R$ 10.000,00")
    BigDecimal valor
) {}
