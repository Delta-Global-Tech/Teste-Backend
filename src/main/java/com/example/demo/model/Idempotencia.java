package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "idempotencia")
public class Idempotencia {

    @EmbeddedId // chave primaria composta
    private IdempotenciaId id;

    @NotBlank
    @Column(name = "hash_requisicao", nullable = false)
    private String hashRequisicao;

    // definição completa para JSON
    @Column(name = "resposta_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String respostaJson;

    @Column(name = "status_http")
    private Integer statusHttp;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public IdempotenciaId getId() {return id;}
    public void setId(IdempotenciaId id) { this.id = id;}

    public String getHashRequisicao() {
        return hashRequisicao;
    }
    public void setHashRequisicao(String hashRequisicao) {
        this.hashRequisicao = hashRequisicao;
    }

    public String getRespostaJson() {
        return respostaJson;
    }
    public void setRespostaJson(String respostaJson) {
        this.respostaJson = respostaJson;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }
    public void setStatusHttp(Integer statusHttp) {
        this.statusHttp = statusHttp;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}