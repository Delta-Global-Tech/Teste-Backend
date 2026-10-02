package com.example.demo.model;

import com.example.demo.enums.TipoMovimento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimento",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_movimento_conta_sequencia",
                        columnNames = {"conta_id", "sequencia"}
                )
        }
)
public class Movimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferencia_id")
    private Transferencia transferencia;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Long sequencia;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private TipoMovimento tipo;

    @NotNull
    @Positive
    @Column(name = "valor_centavos", nullable = false)
    private Long valorCentavos;

    @NotNull
    @Column(name = "saldo_apos_centavos", nullable = false)
    private Long saldoAposCentavos;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public Long getId() {
        return id;
    }

    public Conta getConta() {
        return conta;
    }
    public void setConta(Conta conta) {
        this.conta = conta;
    }

    public Transferencia getTransferencia() {
        return transferencia;
    }
    public void setTransferencia(Transferencia transferencia) {
        this.transferencia = transferencia;
    }

    public Long getSequencia() {
        return sequencia;
    }
    public void setSequencia(Long sequencia) {
        this.sequencia = sequencia;
    }

    public TipoMovimento getTipo() { return tipo; }
    public void setTipo(TipoMovimento tipo) {
        this.tipo = tipo;
    }

    public Long getValorCentavos() {
        return valorCentavos;
    }
    public void setValorCentavos(Long valorCentavos) {
        this.valorCentavos = valorCentavos;
    }

    public Long getSaldoAposCentavos() {
        return saldoAposCentavos;
    }
    public void setSaldoAposCentavos(Long saldoAposCentavos) {
        this.saldoAposCentavos = saldoAposCentavos;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}