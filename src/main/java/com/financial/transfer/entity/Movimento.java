package com.financial.transfer.entity;

import com.financial.transfer.enums.TipoMovimento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "movimento")
@Getter
@Setter
@NoArgsConstructor
public class Movimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id", nullable = false, updatable = false)
    private Conta conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferencia_id", updatable = false)
    private Transferencia transferencia;

    
    @Column(nullable = false, updatable = false)
    private Long sequencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TipoMovimento tipo;

    @Column(name = "valor_centavos", nullable = false, updatable = false)
    private Long valorCentavos;

    @Column(name = "saldo_apos_centavos", nullable = false, updatable = false)
    private Long saldoAposCentavos;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = OffsetDateTime.now();
    }
}
