package com.financial.transfer.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EstornoResponse(
    Long transferenciaEstornoId,
    Long transferenciaOriginalId,
    String contaOrigem,
    String contaDestino,
    BigDecimal valorEstornado,
    BigDecimal taxaEstornada,
    OffsetDateTime concluidaEm
) {}
