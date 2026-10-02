package com.example.demo.dto.usuario;

import com.example.demo.model.Usuario;

public class UsuarioResponseDTO {

    private Long id;
    private String nome;
    private String cpf;

    public UsuarioResponseDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.cpf = usuario.getCpf();
    }

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