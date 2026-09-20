package com.marlonmachado.desafiotecnico.dto;

import java.math.BigDecimal;

public record TransferReceiptDTO(
        Long transferenciaId,
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        BigDecimal taxa,
        BigDecimal totalDebitado,
        BigDecimal saldoOrigemApos,
        String concluidaEm
) {
}
