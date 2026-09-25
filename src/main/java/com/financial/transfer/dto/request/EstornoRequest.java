package com.financial.transfer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstornoRequest(

    @NotBlank(message = "O campo 'motivo' é obrigatório")
    @Size(max = 500, message = "O motivo não pode ultrapassar 500 caracteres")
    String motivo
) {}
