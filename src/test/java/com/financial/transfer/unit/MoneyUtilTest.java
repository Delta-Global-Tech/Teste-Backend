package com.financial.transfer.unit;

import com.financial.transfer.util.MoneyUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoneyUtil — conversões entre reais e centavos")
class MoneyUtilTest {

    @ParameterizedTest(name = "R$ {0} = {1} centavos")
    @CsvSource({
        "100.00, 10000",
        "1.00,   100",
        "0.01,   1",
        "1234.56, 123456",
        "0.50,   50",
        "0.005,  1",    // HALF_UP: 0,5 centavo → 1
        "0.004,  0",    // 0,4 centavo → 0
    })
    @DisplayName("reaisParaCentavos deve converter corretamente")
    void reaisParaCentavos(String reaisStr, long centavosEsperados) {
        long resultado = MoneyUtil.reaisParaCentavos(new BigDecimal(reaisStr));
        assertThat(resultado).isEqualTo(centavosEsperados);
    }

    @ParameterizedTest(name = "{0} centavos = R$ {1}")
    @CsvSource({
        "10000, 100.00",
        "100,   1.00",
        "1,     0.01",
        "123456, 1234.56",
        "50,    0.50",
    })
    @DisplayName("centavosParaReais deve converter corretamente")
    void centavosParaReais(long centavos, String reaisEsperados) {
        BigDecimal resultado = MoneyUtil.centavosParaReais(centavos);
        assertThat(resultado).isEqualByComparingTo(new BigDecimal(reaisEsperados));
    }
}
