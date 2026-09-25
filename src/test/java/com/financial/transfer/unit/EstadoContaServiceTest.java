package com.financial.transfer.unit;

import com.financial.transfer.entity.Conta;
import com.financial.transfer.enums.EstadoConta;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.service.EstadoContaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EstadoContaService — transições de estado de conta")
class EstadoContaServiceTest {

    private final EstadoContaService service = new EstadoContaService();
    private Conta conta;

    @BeforeEach
    void setUp() {
        conta = new Conta();
        conta.setNumero("CONTA-TEST");
        conta.setEstado(EstadoConta.ATIVA);
    }

    // --- Transições válidas ---

    @Test
    @DisplayName("ATIVA → BLOQUEADA: deve ser permitido")
    void ativaParaBloqueada() {
        service.transicionar(conta, EstadoConta.BLOQUEADA);
        assertThat(conta.getEstado()).isEqualTo(EstadoConta.BLOQUEADA);
    }

    @Test
    @DisplayName("ATIVA → ENCERRADA: deve ser permitido")
    void ativaParaEncerrada() {
        service.transicionar(conta, EstadoConta.ENCERRADA);
        assertThat(conta.getEstado()).isEqualTo(EstadoConta.ENCERRADA);
    }

    @Test
    @DisplayName("BLOQUEADA → ATIVA: deve ser permitido")
    void bloqueadaParaAtiva() {
        conta.setEstado(EstadoConta.BLOQUEADA);
        service.transicionar(conta, EstadoConta.ATIVA);
        assertThat(conta.getEstado()).isEqualTo(EstadoConta.ATIVA);
    }

    @Test
    @DisplayName("BLOQUEADA → ENCERRADA: deve ser permitido")
    void bloqueadaParaEncerrada() {
        conta.setEstado(EstadoConta.BLOQUEADA);
        service.transicionar(conta, EstadoConta.ENCERRADA);
        assertThat(conta.getEstado()).isEqualTo(EstadoConta.ENCERRADA);
    }

    // --- Transições inválidas ---

    @Test
    @DisplayName("ENCERRADA → ATIVA: deve lançar exceção (terminal)")
    void encerradaParaAtivaDeveRejeitar() {
        conta.setEstado(EstadoConta.ENCERRADA);
        assertThatThrownBy(() -> service.transicionar(conta, EstadoConta.ATIVA))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Transição inválida");
    }

    @Test
    @DisplayName("ENCERRADA → BLOQUEADA: deve lançar exceção (terminal)")
    void encerradaParaBloqueadaDeveRejeitar() {
        conta.setEstado(EstadoConta.ENCERRADA);
        assertThatThrownBy(() -> service.transicionar(conta, EstadoConta.BLOQUEADA))
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("ATIVA → ATIVA: deve lançar exceção (mesma state)")
    void ativaParaAtivaDeveRejeitar() {
        assertThatThrownBy(() -> service.transicionar(conta, EstadoConta.ATIVA))
            .isInstanceOf(ApiException.class);
    }

    // --- Regras de envio/recebimento ---

    @Test
    @DisplayName("Conta ATIVA pode enviar")
    void ativaPoderEnviar() {
        assertThat(conta.getEstado().podeEnviar()).isTrue();
    }

    @Test
    @DisplayName("Conta BLOQUEADA não pode enviar → validarPodeEnviar lança exceção")
    void bloqueadaNaoPodeEnviar() {
        conta.setEstado(EstadoConta.BLOQUEADA);
        assertThatThrownBy(() -> service.validarPodeEnviar(conta))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("bloqueada");
    }

    @Test
    @DisplayName("Conta ENCERRADA não pode enviar")
    void encerradaNaoPodeEnviar() {
        conta.setEstado(EstadoConta.ENCERRADA);
        assertThatThrownBy(() -> service.validarPodeEnviar(conta))
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("Conta BLOQUEADA pode receber")
    void bloqueadaPodeReceber() {
        conta.setEstado(EstadoConta.BLOQUEADA);
        // não deve lançar exceção
        service.validarPodeReceber(conta);
        assertThat(conta.getEstado()).isEqualTo(EstadoConta.BLOQUEADA);
    }

    @Test
    @DisplayName("Conta ENCERRADA não pode receber → validarPodeReceber lança exceção")
    void encerradaNaoPodeReceber() {
        conta.setEstado(EstadoConta.ENCERRADA);
        assertThatThrownBy(() -> service.validarPodeReceber(conta))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("encerrada");
    }
}
