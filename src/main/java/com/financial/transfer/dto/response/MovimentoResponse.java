package com.financial.transfer.dto.response;

import com.financial.transfer.enums.TipoMovimento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MovimentoResponse(
    Long sequencia,
    TipoMovimento tipo,
    BigDecimal valor,
    BigDecimal saldoApos,
    OffsetDateTime data,
    Long transferenciaId
) {}
