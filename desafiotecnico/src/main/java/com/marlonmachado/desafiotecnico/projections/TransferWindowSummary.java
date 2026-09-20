package com.marlonmachado.desafiotecnico.projections;

import java.math.BigDecimal;

public interface TransferWindowSummary {
    Long getTotalTransferencias();
    Long getValorTransferido();
    Long getTaxas();
    BigDecimal getTicketMedio();
    Long getTotalEstornos();
}