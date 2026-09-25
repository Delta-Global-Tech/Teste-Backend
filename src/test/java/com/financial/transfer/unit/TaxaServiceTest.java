package com.financial.transfer.unit;

import com.financial.transfer.service.TaxaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TaxaService — cálculo de taxa de transferência")
class TaxaServiceTest {

    private final TaxaService taxaService = new TaxaService();

    // Casos obrigatórios do desafio
    @ParameterizedTest(name = "R$ {0} → taxa R$ {1}")
    @CsvSource({
        "1000,  0",      // R$ 10,00   → R$ 0,00
        "10000, 0",      // R$ 100,00  → R$ 0,00
        "10001, 100",    // R$ 100,01  → R$ 1,00 (mínimo aplicado)
        "11250, 113",    // R$ 112,50  → R$ 1,13 (HALF_UP: 1,125 → 1,13)
        "15000, 150",    // R$ 150,00  → R$ 1,50
        "123456, 1235",  // R$ 1.234,56 → R$ 12,35
        "200000, 2000",  // R$ 2.000,00 → R$ 20,00 (máximo)
        "500000, 2000",  // R$ 5.000,00 → R$ 20,00 (máximo aplicado)
    })
    void devCalcularTaxaCorretamente(long valorCentavos, long taxaEsperadaCentavos) {
        assertThat(taxaService.calcular(valorCentavos)).isEqualTo(taxaEsperadaCentavos);
    }

    @Test
    @DisplayName("Valor exatamente no limite de isenção não gera taxa")
    void limiteIsencaoExatoNaoGeraTaxa() {
        assertThat(taxaService.calcular(10000L)).isZero(); // R$ 100,00
    }

    @Test
    @DisplayName("Um centavo acima do limite aplica taxa mínima")
    void umCentavoAcimaDoLimiteAplicaMinimo() {
        assertThat(taxaService.calcular(10001L)).isEqualTo(100L); // R$ 1,00
    }

    @Test
    @DisplayName("Taxa máxima de R$ 20,00 é respeitada para valores muito altos")
    void taxaMaximaRespeitada() {
        assertThat(taxaService.calcular(10_000_000L)).isEqualTo(2000L); // 1% de R$ 100k seria R$ 1000, limitado a R$ 20
    }

    @Test
    @DisplayName("Arredondamento HALF_UP: meio centavo arredonda para cima")
    void arredondamentoHalfUp() {
        // 1% de R$ 112,50 = R$ 1,125 → deve arredondar para R$ 1,13 (HALF_UP)
        long taxa = taxaService.calcular(11250L);
        assertThat(taxa).isEqualTo(113L);
    }

    @Test
    @DisplayName("Valor zero retorna taxa zero")
    void valorZeroRetornaTaxaZero() {
        assertThat(taxaService.calcular(0L)).isZero();
    }

    @Test
    @DisplayName("Valor um centavo retorna taxa zero (abaixo da isenção)")
    void valorUmCentavoRetornaTaxaZero() {
        assertThat(taxaService.calcular(1L)).isZero();
    }
}
