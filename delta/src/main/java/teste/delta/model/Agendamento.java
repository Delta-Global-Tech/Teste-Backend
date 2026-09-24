package teste.delta.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import teste.delta.model.enums.EstadoAgendamento;

@Entity 
@Table (name = "AGENDAMENTO")
public class Agendamento implements Serializable{
    
    @Id 
    @GeneratedValue (strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "CONTA_ORIGEM_ID")
    private UUID contaOrigemId;
    
    @Column(name = "CONTA_DESTINO_ID")
    private UUID contaDestinoId;
    
    @Column(name = "VALOR_CENTAVOS")
    @JdbcTypeCode(SqlTypes.BIGINT)
    private BigDecimal valorCentavos;

    @Column(name = "EXECUTAR_EM")
    private OffsetDateTime executarEm;

    @Enumerated (EnumType.STRING)
    @Column(name = "ESTADO")
    private EstadoAgendamento estado;
    
    @Column(name = "TENTATIVAS")
    private int tentativas;
    
    @Column(name = "TRANSFERENCIA_ID")
    private UUID transferenciaId;


    public Agendamento() {
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

    public OffsetDateTime getExecutarEm() {
        return executarEm;
    }

    public void setExecutarEm(OffsetDateTime executarEm) {
        this.executarEm = executarEm;
    }

    public EstadoAgendamento getEstado() {
        return estado;
    }

    public void setEstado(EstadoAgendamento estado) {
        this.estado = estado;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void setTentativas(int tentativas) {
        this.tentativas = tentativas;
    }

    public UUID getTransferenciaId() {
        return transferenciaId;
    }

    public void setTransferenciaId(UUID transferenciaId) {
        this.transferenciaId = transferenciaId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((contaOrigemId == null) ? 0 : contaOrigemId.hashCode());
        result = prime * result + ((contaDestinoId == null) ? 0 : contaDestinoId.hashCode());
        result = prime * result + ((valorCentavos == null) ? 0 : valorCentavos.hashCode());
        result = prime * result + ((executarEm == null) ? 0 : executarEm.hashCode());
        result = prime * result + ((estado == null) ? 0 : estado.hashCode());
        result = prime * result + tentativas;
        result = prime * result + ((transferenciaId == null) ? 0 : transferenciaId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Agendamento other = (Agendamento) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (contaOrigemId == null) {
            if (other.contaOrigemId != null)
                return false;
        } else if (!contaOrigemId.equals(other.contaOrigemId))
            return false;
        if (contaDestinoId == null) {
            if (other.contaDestinoId != null)
                return false;
        } else if (!contaDestinoId.equals(other.contaDestinoId))
            return false;
        if (valorCentavos == null) {
            if (other.valorCentavos != null)
                return false;
        } else if (!valorCentavos.equals(other.valorCentavos))
            return false;
        if (executarEm == null) {
            if (other.executarEm != null)
                return false;
        } else if (!executarEm.equals(other.executarEm))
            return false;
        if (estado != other.estado)
            return false;
        if (tentativas != other.tentativas)
            return false;
        if (transferenciaId == null) {
            if (other.transferenciaId != null)
                return false;
        } else if (!transferenciaId.equals(other.transferenciaId))
            return false;
        return true;
    }

    

    
}
