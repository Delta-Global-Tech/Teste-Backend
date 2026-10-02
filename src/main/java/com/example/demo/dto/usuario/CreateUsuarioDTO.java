package com.example.demo.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUsuarioDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 30, message = "Nome deve ter no máximo 30 caracteres")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(
            regexp = "\\d{11}",
            message = "CPF deve conter exatamente 11 números"
    )
    private String cpf;

    public CreateUsuarioDTO(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
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
