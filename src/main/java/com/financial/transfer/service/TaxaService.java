package com.financial.transfer.service;

import org.springframework.stereotype.Service;


@Service
public class TaxaService {

    private static final long LIMITE_ISENCAO_CENTAVOS = 10_000L;   // R$ 100,00
    private static final long TAXA_MINIMA_CENTAVOS    =   100L;    // R$ 1,00
    private static final long TAXA_MAXIMA_CENTAVOS    = 2_000L;    // R$ 20,00

    public long calcular(long valorCentavos) {
        if (valorCentavos <= LIMITE_ISENCAO_CENTAVOS) {
            return 0L;
        }
        
        long taxa = (valorCentavos + 50) / 100;

        if (taxa < TAXA_MINIMA_CENTAVOS) taxa = TAXA_MINIMA_CENTAVOS;
        if (taxa > TAXA_MAXIMA_CENTAVOS) taxa = TAXA_MAXIMA_CENTAVOS;

        return taxa;
    }
}
