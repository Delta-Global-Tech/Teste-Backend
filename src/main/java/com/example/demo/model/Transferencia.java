package com.example.demo.model;

import com.example.demo.enums.EstadoTransferencia;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "transferencia")
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_origem_id", nullable = false)
    private Conta contaOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_destino_id", nullable = false)
    private Conta contaDestino;

    @Column(name = "valor_centavos", nullable = false)
    private Long valorCentavos;

    @Column(name = "taxa_centavos", nullable = false)
    private Long taxaCentavos = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoTransferencia estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferencia_original_id")
    private Transferencia transferenciaOriginal;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "concluida_em")
    private Instant concluidaEm;

    public Transferencia() {}

    public Transferencia(
            Conta contaOrigem,
            Conta contaDestino,
            Long valorCentavos,
            Long taxaCentavos,
            EstadoTransferencia estado) {
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valorCentavos = valorCentavos;
        this.taxaCentavos = taxaCentavos;
        this.criadaEm = Instant.now();
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public Conta getContaOrigem() {
        return contaOrigem;
    }

    public void setContaOrigem(Conta contaOrigem) {
        this.contaOrigem = contaOrigem;
    }

    public Conta getContaDestino() {
        return contaDestino;
    }

    public void setContaDestino(Conta contaDestino) {
        this.contaDestino = contaDestino;
    }

    public Long getValorCentavos() {
        return valorCentavos;
    }

    public void setValorCentavos(Long valorCentavos) {
        this.valorCentavos = valorCentavos;
    }

    public Long getTaxaCentavos() {
        return taxaCentavos;
    }

    public void setTaxaCentavos(Long taxaCentavos) {
        this.taxaCentavos = taxaCentavos;
    }

    public EstadoTransferencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoTransferencia novoEstado) {

        boolean permitida =
                (this.estado == EstadoTransferencia.CRIADA &&
                        (novoEstado == EstadoTransferencia.CONFIRMADA ||
                                novoEstado == EstadoTransferencia.FALHADA))
                        ||
                        (this.estado == EstadoTransferencia.CONFIRMADA &&
                                novoEstado == EstadoTransferencia.ESTORNADA);

        if (!permitida) {
            throw new IllegalStateException(
                    "Transição de estado inválida"
            );
        }

        this.estado = novoEstado;
    }

    public Transferencia getTransferenciaOriginal() {
        return transferenciaOriginal;
    }

    public void setTransferenciaOriginal(Transferencia transferenciaOriginal) {
        this.transferenciaOriginal = transferenciaOriginal;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(Instant criadaEm) {
        this.criadaEm = criadaEm;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }

    public void setConcluidaEm(Instant concluidaEm) {
        this.concluidaEm = concluidaEm;
    }
}