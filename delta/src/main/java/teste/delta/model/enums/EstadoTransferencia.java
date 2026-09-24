package teste.delta.model.enums;

import java.util.Arrays;

public enum EstadoTransferencia {
    CRIADA,
    CONFIRMADA,
    ESTORNADA,
    FALHADA;

    public static EstadoTransferencia from(String value) {
        if (value == null || value.isBlank()){
            return null;
        }
        return Arrays.stream(values())
            .filter(v -> v.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid account status + " + value));
    }

    public static String toStringValue(EstadoTransferencia estadoTransferencia) {
        return estadoTransferencia != null ? estadoTransferencia.name() : null;
    }
}
