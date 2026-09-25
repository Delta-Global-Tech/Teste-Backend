package com.financial.transfer.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransferenciaResponse(
    Long transferenciaId,
    String contaOrigem,
    String contaDestino,
    BigDecimal valor,
    BigDecimal taxa,
    BigDecimal totalDebitado,
    BigDecimal saldoOrigemApos,
    OffsetDateTime concluidaEm
) {}
