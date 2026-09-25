package com.financial.transfer.entity;

import com.financial.transfer.enums.EstadoTransferencia;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "transferencia")
@Getter
@Setter
@NoArgsConstructor
public class Transferencia {

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

    @Column(name = "taxa_centavos", nullable = false)
    private Long taxaCentavos = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTransferencia estado = EstadoTransferencia.CRIADA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferencia_original_id")
    private Transferencia transferenciaOriginal;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private OffsetDateTime criadaEm;

    @Column(name = "concluida_em")
    private OffsetDateTime concluidaEm;

    @PrePersist
    protected void onCreate() {
        this.criadaEm = OffsetDateTime.now();
    }

}
