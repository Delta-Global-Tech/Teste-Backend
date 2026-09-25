package com.financial.transfer.entity;

import com.financial.transfer.enums.EstadoAgendamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "agendamento")
@Getter
@Setter
@NoArgsConstructor
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
    private OffsetDateTime executarEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAgendamento estado = EstadoAgendamento.PENDENTE;

    @Column(nullable = false)
    private Integer tentativas = 0;

    /**
     * Transferência gerada após a execução bem-sucedida.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferencia_id")
    private Transferencia transferencia;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    /**
     * Timestamp de quando o job adquiriu este agendamento.
     * Usado para detectar agendamentos travados: se processando_em
     * for mais antigo que N minutos e estado ainda for PENDENTE,
     */
    @Column(name = "processando_em")
    private OffsetDateTime processandoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = OffsetDateTime.now();
    }
}
