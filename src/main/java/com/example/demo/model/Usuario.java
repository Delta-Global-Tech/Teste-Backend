package com.example.demo.model;

import com.example.demo.dto.usuario.CreateUsuarioDTO;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 30)
    private String nome;

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    public Usuario(CreateUsuarioDTO createUsuarioDTO) {
        this.nome = createUsuarioDTO.getNome();
        this.cpf = createUsuarioDTO.getCpf();
    }

    protected Usuario() {}

    public Long getId() {
        return id;
    }
    public String getNome() {
        return nome;
    }
    public String getCpf() {
        return cpf;
    }
}