package com.financial.transfer.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DepositoResponse(
    Long movimentoId,
    String conta,
    BigDecimal valor,
    BigDecimal saldoApos,
    OffsetDateTime criadoEm
) {}
