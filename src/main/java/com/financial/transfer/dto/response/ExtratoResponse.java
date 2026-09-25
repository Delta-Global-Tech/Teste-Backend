package com.financial.transfer.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExtratoResponse(
    String conta,
    BigDecimal saldoAtual,
    long totalMovimentos,
    int page,
    int size,
    int totalPages,
    List<MovimentoResponse> movimentos
) {}
