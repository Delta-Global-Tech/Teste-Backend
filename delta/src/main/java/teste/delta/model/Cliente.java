package teste.delta.model;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "CLIENTE")
public class Cliente implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "NAME", nullable = false)
    private String nome;

    @Column(name = "CPF", nullable = false)
    private String cpf;


    public Cliente() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    
}
