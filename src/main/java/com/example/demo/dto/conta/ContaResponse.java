package com.example.demo.dto.conta;

import com.example.demo.enums.EstadoConta;
import com.example.demo.model.Conta;

public class ContaResponse {

    private Long id;
    private String numero;
    private Long usuarioId;
    private Long saldoCentavos;
    private Long limiteDiarioCentavos;
    private EstadoConta estado;

    public ContaResponse() {}

    public ContaResponse(Conta conta) {
        this.id = conta.getId();
        this.numero = conta.getNumero();
        this.usuarioId = conta.getUsuario().getId();
        this.saldoCentavos = conta.getSaldoCentavos();
        this.limiteDiarioCentavos = conta.getLimiteDiarioCentavos();
        this.estado = conta.getEstado();
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getSaldoCentavos() {
        return saldoCentavos;
    }

    public Long getLimiteDiarioCentavos() {
        return limiteDiarioCentavos;
    }

    public EstadoConta getEstado() {
        return estado;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void setSaldoCentavos(Long saldoCentavos) {
        this.saldoCentavos = saldoCentavos;
    }

    public void setLimiteDiarioCentavos(Long limiteDiarioCentavos) {
        this.limiteDiarioCentavos = limiteDiarioCentavos;
    }

    public void setEstado(EstadoConta estado) {
        this.estado = estado;
    }
}