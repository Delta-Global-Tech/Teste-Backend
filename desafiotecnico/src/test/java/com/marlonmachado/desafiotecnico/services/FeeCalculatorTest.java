package com.marlonmachado.desafiotecnico.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


public class FeeCalculatorTest {

    private final FeeCalculator feeCalculator = new FeeCalculator();

    @ParameterizedTest
    @CsvSource({
            "1000, 0",      // R$ 10,00 -> R$ 0,00
            "10000, 0",     // R$ 100,00 -> R$ 0,00
            "10001, 100",   // R$ 100,01 -> R$ 1,00
            "11250, 113",   // R$ 112,50 -> R$ 1,13
            "15000, 150",   // R$ 150,00 -> R$ 1,50
            "123456, 1235", // R$ 1.234,56 -> R$ 12,35
            "200000, 2000", // R$ 2.000,00 -> R$ 20,00
            "500000, 2000"  // R$ 5.000,00 -> R$ 20,00 (teto)
    })
    @DisplayName("Deve calcular a taxa corretamente para diferentes valores de transferência")
    void shouldCalculateFeeCorrectly(long amountCents, long expectedFeeCents) {
        long actualFeeCents = feeCalculator.calculate(amountCents);
       Assertions.assertEquals(expectedFeeCents, actualFeeCents,
                "A taxa calculada para " + amountCents + " centavos deveria ser " + expectedFeeCents);
    }

    @Test
    @DisplayName("Deve retornar 0 para valores negativos ou zero")
    void shouldReturnZeroForNegativeOrZeroAmount() {
        Assertions.assertEquals(0L, feeCalculator.calculate(0L));
        Assertions.assertEquals(0L, feeCalculator.calculate(-100L));
    }
}
