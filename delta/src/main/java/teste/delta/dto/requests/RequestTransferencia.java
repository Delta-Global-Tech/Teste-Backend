package teste.delta.dto.requests;

import java.math.BigDecimal;

public record RequestTransferencia(
    String contaOrigem,
    String contaDestino,
    BigDecimal valor
) {
    
}
