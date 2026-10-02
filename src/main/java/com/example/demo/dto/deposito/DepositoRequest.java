package com.example.demo.dto.deposito;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DepositoRequest {

    @NotNull
    @Positive
    private Long valorCentavos;

    public Long getValorCentavos() {
        return valorCentavos;
    }

    public void setValorCentavos(Long valorCentavos) {
        this.valorCentavos = valorCentavos;
    }
}
