package com.financial.transfer.util;

import java.math.BigDecimal;
import java.math.RoundingMode;


public final class MoneyUtil {

    private MoneyUtil() {}

    private static final BigDecimal CENTAVOS_POR_REAL = new BigDecimal("100");

    public static long reaisParaCentavos(BigDecimal reais) {
        return reais
            .multiply(CENTAVOS_POR_REAL)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    public static BigDecimal centavosParaReais(long centavos) {
        return BigDecimal.valueOf(centavos)
            .divide(CENTAVOS_POR_REAL)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal centavosParaReais(Double centavos) {
        if (centavos == null || centavos == 0.0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(centavos)
            .divide(CENTAVOS_POR_REAL)
            .setScale(2, RoundingMode.HALF_UP);
    }
}
