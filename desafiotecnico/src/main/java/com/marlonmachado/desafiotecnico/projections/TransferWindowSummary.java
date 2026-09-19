package com.marlonmachado.desafiotecnico.projections;

public interface TransferWindowSummary {
    Long getTotalTransferencias();
    Long getValorTransferido();
    Long getTaxas();
    Double getTicketMedio();
    Long getTotalEstornos();
}