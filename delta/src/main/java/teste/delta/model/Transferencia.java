package teste.delta.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import teste.delta.model.enums.EstadoTransferencia;

@Entity
@Table (name = "TRANSFERENCIA")
public class Transferencia implements Serializable{
    
    @Id
    @GeneratedValue (strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "CONTA_ORIGEM_ID", nullable = false)
    private UUID contaOrigemId;
    
    @Column(name = "CONTA_DESTINO_ID", nullable = false)
    private UUID contaDestinoId;
    
    @Column(name = "VALOR_CENTAVOS", nullable = false)
    @JdbcTypeCode(SqlTypes.BIGINT)
    private BigDecimal valorCentavos;
    
    @Column(name = "TAXA_CENTAVOS", nullable = false)
    private int taxaCentavos;
    
    @Enumerated (EnumType.STRING)
    @Column(name = "ESTADO", nullable = false)
    private EstadoTransferencia estado;
    
    @Column (name = "TRANSFERENCIA_ORIGINAL_ID", nullable = true)
    private UUID transferenciaOriginal; // (só em estorno);
    
    @Column(name = "CRIADA_EM", nullable = false)
    private OffsetDateTime criadaEm;
    
    @Column(name = "CONCLUIDA_EM")
    private OffsetDateTime concluidaEm;

    public Transferencia() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getContaOrigemId() {
        return contaOrigemId;
    }

    public void setContaOrigemId(UUID contaOrigemId) {
        this.contaOrigemId = contaOrigemId;
    }

    public UUID getContaDestinoId() {
        return contaDestinoId;
    }

    public void setContaDestinoId(UUID contaDestinoId) {
        this.contaDestinoId = contaDestinoId;
    }

    public BigDecimal getValorCentavos() {
        return valorCentavos;
    }

    public void setValorCentavos(BigDecimal valorCentavos) {
        this.valorCentavos = valorCentavos;
    }

    public int getTaxaCentavos() {
        return taxaCentavos;
    }

    public void setTaxaCentavos(int taxaCentavos) {
        this.taxaCentavos = taxaCentavos;
    }

    public EstadoTransferencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoTransferencia estado) {
        this.estado = estado;
    }

    public UUID getTransferenciaOriginalId() {
        return transferenciaOriginal;
    }

    public void setTransferenciaOriginalId(UUID transferenciaOriginalId) {
        this.transferenciaOriginal = transferenciaOriginalId;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(OffsetDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public OffsetDateTime getConcluidaEm() {
        return concluidaEm;
    }

    public void setConcluidaEm(OffsetDateTime concluidaEm) {
        this.concluidaEm = concluidaEm;
    }

    
}
