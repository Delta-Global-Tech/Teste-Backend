package com.example.demo.dto.transferencia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TransferenciaRequest {

    @NotNull(message = "Conta de origem é obrigatória")
    private Long contaOrigemId;

    @NotNull(message = "Conta de destino é obrigatória")
    private Long contaDestinoId;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser maior que zero")
    private Long valorCentavos;

    public Long getContaOrigemId() {
        return contaOrigemId;
    }
    public void setContaOrigemId(Long contaOrigemId) {
        this.contaOrigemId = contaOrigemId;
    }

    public Long getContaDestinoId() {
        return contaDestinoId;
    }
    public void setContaDestinoId(Long contaDestinoId) {
        this.contaDestinoId = contaDestinoId;
    }

    public Long getValorCentavos() {
        return valorCentavos;
    }
    public void setValorCentavos(Long valorCentavos) { this.valorCentavos = valorCentavos; }
}