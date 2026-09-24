package teste.delta.model;

import java.io.Serializable;
import java.util.Objects;

public class IdempotenciaId implements Serializable {

    private String chave;

    private String endpoint;

    public IdempotenciaId() {
    }

    public IdempotenciaId(String chave, String endpoint) {
        this.chave = chave;
        this.endpoint = endpoint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chave, endpoint);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof IdempotenciaId other))
            return false;
        return Objects.equals(chave, other.chave) && Objects.equals(endpoint, other.endpoint);
    }
}
