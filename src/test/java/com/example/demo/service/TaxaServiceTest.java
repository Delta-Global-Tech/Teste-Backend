package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxaServiceTest {

    private final TaxaService taxaService = new TaxaService();

    @Test
    void deveSerZeroParaValorAteCemReais() {
        assertEquals(0, taxaService.calcularTaxa(10_000));
    }

    @Test
    void deveCalcularUmPorCento() {
        assertEquals(113, taxaService.calcularTaxa(11_250));
    }

    @Test
    void deveRespeitarTaxaMinima() {
        assertEquals(100, taxaService.calcularTaxa(10_001));
    }

    @Test
    void deveRespeitarTaxaMaxima() {
        assertEquals(2_000, taxaService.calcularTaxa(200_001));
    }
}