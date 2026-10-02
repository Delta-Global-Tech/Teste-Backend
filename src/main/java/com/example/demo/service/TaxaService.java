package com.example.demo.service;

import org.springframework.stereotype.Service;

@Service
public class TaxaService {

    public long calcularTaxa(long valorCentavos) {

        // Até R$ 100,00: isento
        if (valorCentavos <= 10_000) {
            return 0;
        }

        // 1%, arredondando meio centavo para cima
        long taxa = (valorCentavos + 50) / 100;

        // Mínimo de R$ 1,00
        taxa = Math.max(taxa, 100);

        // Máximo de R$ 20,00
        taxa = Math.min(taxa, 2_000);

        return taxa;
    }
}