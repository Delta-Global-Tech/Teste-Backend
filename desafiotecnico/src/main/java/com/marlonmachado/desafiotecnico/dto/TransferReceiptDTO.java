package com.marlonmachado.desafiotecnico.dto;

public record TransferReceiptDTO(
        Long transferenciaId,
        String contaOrigem,
        String contaDestino,
        Double valor,
        Double taxa,
        Double totalDebitado,
        Double saldoOrigemApos,
        String concluidaEm
) {
}
