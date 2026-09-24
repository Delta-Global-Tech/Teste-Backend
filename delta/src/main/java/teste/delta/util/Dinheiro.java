package teste.delta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.http.HttpStatus;

import teste.delta.exception.ApiException;

/**
 * Conversão entre reais (formato da API) e centavos (formato do banco). Nada de float/double.
 */
public final class Dinheiro {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private Dinheiro() {
    }

    public static BigDecimal paraCentavos(BigDecimal reais){
        if (reais == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALOR_OBRIGATORIO", "O campo 'valor' é obrigatório");
        }
        try {
            return reais.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALOR_INVALIDO",
                "O valor precisa ter no máximo duas casas decimais"
            );
        }
    }

    public static BigDecimal paraReais(BigDecimal centavos){
        return centavos.movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal paraReais(long centavos){
        return paraReais(BigDecimal.valueOf(centavos));
    }

    public static String formatar(BigDecimal centavos){
        return NumberFormat.getCurrencyInstance(PT_BR).format(paraReais(centavos));
    }
}
