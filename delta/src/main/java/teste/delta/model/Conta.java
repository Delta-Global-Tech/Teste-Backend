package teste.delta.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import teste.delta.model.enums.EstadoConta;


@Entity
@Table (name = "CONTA")
public class Conta implements Serializable{

    @Id
    @GeneratedValue (strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column (name = "NUMERO", nullable = false)
    private String numero;

    @Column (name = "USUARIO_ID", nullable = true)
    private UUID usuarioId;

    @Column (name = "SALDO_CENTAVOS", nullable = false)
    @JdbcTypeCode(SqlTypes.BIGINT)
    private BigDecimal saldoCentavos;

    @Column (name = "LIMITE_DIARIO_CENTAVOS", nullable = false)
    private int limiteDiarioCentavos;

    @Enumerated (EnumType.STRING)
    @Column (name = "ESTADO", nullable = false)
    private EstadoConta estado;

    @Version 
    @Column (name = "VERSAO", nullable = false)
    private Long versao;

    

    public Conta() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public BigDecimal getSaldoCentavos() {
        return saldoCentavos;
    }

    public void setSaldoCentavos(BigDecimal saldoCentavos) {
        this.saldoCentavos = saldoCentavos;
    }

    public int getLimiteDiarioCentavos() {
        return limiteDiarioCentavos;
    }

    public void setLimiteDiarioCentavos(int limiteDiarioCentavos) {
        this.limiteDiarioCentavos = limiteDiarioCentavos;
    }

    public EstadoConta getEstado() {
        return estado;
    }

    public void setEstado(EstadoConta estado) {
        this.estado = estado;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((numero == null) ? 0 : numero.hashCode());
        result = prime * result + ((usuarioId == null) ? 0 : usuarioId.hashCode());
        result = prime * result + ((saldoCentavos == null) ? 0 : saldoCentavos.hashCode());
        result = prime * result + limiteDiarioCentavos;
        result = prime * result + ((estado == null) ? 0 : estado.hashCode());
        result = prime * result + ((versao == null) ? 0 : versao.hashCode());
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
        Conta other = (Conta) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (numero == null) {
            if (other.numero != null)
                return false;
        } else if (!numero.equals(other.numero))
            return false;
        if (usuarioId == null) {
            if (other.usuarioId != null)
                return false;
        } else if (!usuarioId.equals(other.usuarioId))
            return false;
        if (saldoCentavos == null) {
            if (other.saldoCentavos != null)
                return false;
        } else if (!saldoCentavos.equals(other.saldoCentavos))
            return false;
        if (limiteDiarioCentavos != other.limiteDiarioCentavos)
            return false;
        if (estado != other.estado)
            return false;
        if (versao == null) {
            if (other.versao != null)
                return false;
        } else if (!versao.equals(other.versao))
            return false;
        return true;
    }

    
}
