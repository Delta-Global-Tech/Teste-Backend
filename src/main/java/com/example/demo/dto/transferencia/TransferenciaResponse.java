package com.example.demo.dto.transferencia;

import com.example.demo.model.Transferencia;

import java.time.Instant;

public class TransferenciaResponse {

    private Long id;
    private Long contaOrigemId;
    private Long contaDestinoId;
    private Long valorCentavos;
    private Long taxaCentavos;
    private String estado;
    private Instant criadaEm;
    private Instant concluidaEm;

    public TransferenciaResponse() {}

    public TransferenciaResponse(
            Transferencia transferencia
    ) {
        this.id = transferencia.getId();
        this.contaOrigemId = transferencia.getContaOrigem().getId();
        this.contaDestinoId = transferencia.getContaDestino().getId();
        this.valorCentavos = transferencia.getValorCentavos();
        this.taxaCentavos = transferencia.getTaxaCentavos();
        this.estado = transferencia.getEstado().toString();
        this.criadaEm = transferencia.getCriadaEm();
        this.concluidaEm = transferencia.getConcluidaEm();
    }

    public Long getId() {
        return id;
    }

    public Long getContaOrigemId() {return contaOrigemId;}

    public Long getContaDestinoId() {
        return contaDestinoId;
    }

    public Long getValorCentavos() {
        return valorCentavos;
    }

    public Long getTaxaCentavos() {
        return taxaCentavos;
    }

    public String getEstado() {
        return estado;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }
}