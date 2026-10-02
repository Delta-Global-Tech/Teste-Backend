package com.example.demo.dto.estorno;

import jakarta.validation.constraints.NotBlank;

public class EstornoRequest {

    @NotBlank
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}