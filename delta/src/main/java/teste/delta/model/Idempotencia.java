package teste.delta.model;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table (name = "IDEMPOTENCIA")
@IdClass (IdempotenciaId.class)
public class Idempotencia implements Serializable{

    @Id
    @Column( name = "CHAVE")
    private String chave;


    @Id
    @Column( name = "ENDPOINT")
    private String endpoint;


    @Column(name = "HASH_REQUISICAO", nullable = false)
    private String hashRequisicao;


    @Column(name = "RESPOSTA_JSON", nullable = false)
    private String respostaJson;


    @Column(name = "STATUS_HTTP", nullable = false)
    private int statusHttp;


    @Column(name = "CRIADO_EM", nullable = false)
    private OffsetDateTime criadoEm;


    public Idempotencia() {
    }


    public String getChave() {
        return chave;
    }


    public void setChave(String chave) {
        this.chave = chave;
    }


    public String getEndpoint() {
        return endpoint;
    }


    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }


    public String getHashRequisicao() {
        return hashRequisicao;
    }


    public void setHashRequisicao(String hashRequisicao) {
        this.hashRequisicao = hashRequisicao;
    }


    public String getRespostaJson() {
        return respostaJson;
    }


    public void setRespostaJson(String respostaJson) {
        this.respostaJson = respostaJson;
    }


    public int getStatusHttp() {
        return statusHttp;
    }


    public void setStatusHttp(int statusHttp) {
        this.statusHttp = statusHttp;
    }


    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }


    public void setCriadoEm(OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }


    
}
