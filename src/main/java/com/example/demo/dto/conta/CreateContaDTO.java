package com.example.demo.dto.conta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class CreateContaDTO {

    @NotBlank(message = "Número da conta é obrigatório")
    @Size(max = 9, message = "Número da conta deve ter no máximo 9 caracteres")
    private String numero;

    @NotNull(message = "Usuário é obrigatório")
    private Long usuarioId;

    @NotNull(message = "Limite diário é obrigatório")
    @PositiveOrZero(message = "Limite diário não pode ser negativo")
    private Long limiteDiarioCentavos;

    public String getNumero() {
        return numero;
    }
    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }
    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getLimiteDiarioCentavos() {
        return limiteDiarioCentavos;
    }
    public void setLimiteDiarioCentavos(Long limiteDiarioCentavos) {
        this.limiteDiarioCentavos = limiteDiarioCentavos;
    }
}