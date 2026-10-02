package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Entity
@Table(name = "agendamento")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_origem_id", nullable = false)
    private Conta contaOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_destino_id", nullable = false)
    private Conta contaDestino;

    @Column(name = "valor_centavos", nullable = false)
    private Long valorCentavos;

    @Column(name = "executar_em", nullable = false)
    private LocalDateTime executarEm;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(nullable = false)
    private Long tentativas = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferencia_id")
    private Transferencia transferencia;

    public Long getId() { return id; }

    public Conta getContaOrigem() { return contaOrigem; }
    public void setContaOrigem(Conta contaOrigem) { this.contaOrigem = contaOrigem; }

    public Conta getContaDestino() { return contaDestino; }
    public void setContaDestino(Conta contaDestino) { this.contaDestino = contaDestino; }

    public Long getValorCentavos() { return valorCentavos; }
    public void setValorCentavos(Long valorCentavos) { this.valorCentavos = valorCentavos; }

    public LocalDateTime getExecutarEm() { return executarEm; }
    public void setExecutarEm(LocalDateTime executarEm) { this.executarEm = executarEm;}

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Long getTentativas() { return tentativas;}
    public void setTentativas(Long tentativas) { this.tentativas = tentativas;}

    public Transferencia getTransferencia() { return transferencia; }
    public void setTransferencia(Transferencia transferencia) { this.transferencia = transferencia;}
}