package teste.delta.model.enums;

import java.util.Arrays;

public enum TipoMovimento {
    ENTRADA,
    SAIDA;

    public static TipoMovimento from(String value) {
        if (value == null || value.isBlank()){
            return null;
        }
        return Arrays.stream(values())
            .filter(v -> v.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid account status + " + value));
    }

    public static String toStringValue(TipoMovimento tipoMovimento) {
        return tipoMovimento != null ? tipoMovimento.name() : null;
    }
}
