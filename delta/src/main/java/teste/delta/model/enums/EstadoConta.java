package teste.delta.model.enums;

import java.util.Arrays;

public enum EstadoConta {
    ATIVA,
    BLOQUEADA,
    ENCERRADA;

    public static EstadoConta from(String value) {
        if (value == null || value.isBlank()){
            return null;
        }
        return Arrays.stream(values())
            .filter(v -> v.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid account status + " + value));
    }

    public static String toStringValue(EstadoConta estadoConta) {
        return estadoConta != null ? estadoConta.name() : null;
    }
}
