package com.example.demo.model;

import com.example.demo.dto.conta.CreateContaDTO;
import com.example.demo.enums.EstadoConta;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "conta")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 9)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "saldo_centavos", nullable = false)
    private Long saldoCentavos = 0L;

    @Column(name = "limite_diario_centavos", nullable = false)
    private Long limiteDiarioCentavos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoConta estado;

    @Column(nullable = false)
    private Long versao = 0L;

    public Long getId() {
        return id;
    }

    public Conta (CreateContaDTO createContaDTO, Usuario usuario) {
        this.numero = createContaDTO.getNumero();
        this.usuario = usuario;
        this.limiteDiarioCentavos = createContaDTO.getLimiteDiarioCentavos();
    }

    public Conta () {

    }

    public String getNumero() {
        return numero;
    }
    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Usuario getUsuario() {
        return usuario;
    }
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Long getSaldoCentavos() {
        return saldoCentavos;
    }
    public void setSaldoCentavos(Long saldoCentavos) {
        this.saldoCentavos = saldoCentavos;
    }

    public Long getLimiteDiarioCentavos() {
        return limiteDiarioCentavos;
    }
    public void setLimiteDiarioCentavos(Long limiteDiarioCentavos) {
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
}