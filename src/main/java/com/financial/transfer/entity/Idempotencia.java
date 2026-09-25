package com.financial.transfer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "idempotencia",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_idempotencia_endpoint_chave",
           columnNames = {"endpoint", "chave"}
       ))
@Getter
@Setter
@NoArgsConstructor
public class Idempotencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String chave;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "hash_requisicao", nullable = false, length = 64)
    private String hashRequisicao;

    @Column(name = "resposta_json", nullable = false, columnDefinition = "TEXT")
    private String respostaJson;

    @Column(name = "status_http", nullable = false)
    private Integer statusHttp;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = OffsetDateTime.now();
    }
}
