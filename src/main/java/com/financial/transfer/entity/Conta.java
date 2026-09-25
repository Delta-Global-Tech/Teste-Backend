package com.financial.transfer.entity;

import com.financial.transfer.enums.EstadoConta;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conta")
@Getter
@Setter

@NoArgsConstructor
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

   
    @Column(name = "saldo_centavos", nullable = false)
    private Long saldoCentavos = 0L;

   
    @Column(name = "limite_diario_centavos", nullable = false)
    private Long limiteDiarioCentavos = 200000L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConta estado = EstadoConta.ATIVA;

    @Version
    @Column(nullable = false)
    private Long versao = 0L;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private OffsetDateTime criadaEm;

    @PrePersist
    protected void onCreate() {
        this.criadaEm = OffsetDateTime.now();
    }

    public boolean isContaSistema() {
        return this.usuario == null;
    }
}
