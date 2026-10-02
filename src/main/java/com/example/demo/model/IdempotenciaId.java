package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

// cria chave primary composta
@Embeddable
public class IdempotenciaId implements Serializable {

    @Column(nullable = false, length = 100)
    private String endpoint;

    @Column(nullable = false, length = 255)
    private String chave;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdempotenciaId that)) return false;
        return Objects.equals(endpoint, that.endpoint)
                && Objects.equals(chave, that.chave);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, chave);
    }
}