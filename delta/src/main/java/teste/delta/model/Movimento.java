package teste.delta.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import teste.delta.model.enums.TipoMovimento;

@Entity 
@Table (name = "MOVIMENTO")
public class Movimento implements Serializable{

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "CONTA_ID")
    private UUID contaId;

    @Column (name = "TRANSFERENCIA_ID")
    private UUID transferenciaId;

    @Column (name = "SEQUENCIA", nullable = false, updatable = false)
    private Long sequencia;

    @Column (name = "TAXA", nullable = false, updatable = false)
    private boolean taxa;

    @Enumerated (EnumType.STRING)
    @Column (name = "TIPO_MOVIMENTO", nullable = false)
    private TipoMovimento tipo;

    @Column (name = "VALOR_CENTAVOS", nullable = false)
    @JdbcTypeCode(SqlTypes.BIGINT)
    private BigDecimal valorCentavos;

    @Column (name = "SALDO_APOS_CENTAVOS", nullable = false)
    @JdbcTypeCode(SqlTypes.BIGINT)
    private BigDecimal saldoAposCentavos;

    @Column (name = "CRIADO_EM", nullable = false)
    private OffsetDateTime criadoEm;

    public Movimento() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public void setContaId(UUID contaId) {
        this.contaId = contaId;
    }

    public UUID getTransferenciaId() {
        return transferenciaId;
    }

    public void setTransferenciaId(UUID transferenciaId) {
        this.transferenciaId = transferenciaId;
    }

    public Long getSequencia() {
        return sequencia;
    }

    public void setSequencia(Long sequencia) {
        this.sequencia = sequencia;
    }

    public boolean isTaxa() {
        return taxa;
    }

    public void setTaxa(boolean taxa) {
        this.taxa = taxa;
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimento tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValorCentavos() {
        return valorCentavos;
    }

    public void setValorCentavos(BigDecimal valorCentavos) {
        this.valorCentavos = valorCentavos;
    }

    public BigDecimal getSaldoAposCentavos() {
        return saldoAposCentavos;
    }

    public void setSaldoAposCentavos(BigDecimal saldoAposCentavos) {
        this.saldoAposCentavos = saldoAposCentavos;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    
    
}
