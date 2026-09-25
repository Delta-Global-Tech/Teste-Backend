package com.financial.transfer.unit;

import com.financial.transfer.entity.Transferencia;
import com.financial.transfer.enums.EstadoTransferencia;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.service.EstadoTransferenciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EstadoTransferenciaService — transições de estado de transferência")
class EstadoTransferenciaServiceTest {

    private final EstadoTransferenciaService service = new EstadoTransferenciaService();
    private Transferencia transferencia;

    @BeforeEach
    void setUp() {
        transferencia = new Transferencia();
        transferencia.setEstado(EstadoTransferencia.CRIADA);
    }

    @Test
    @DisplayName("CRIADA → CONFIRMADA: válido")
    void criadaParaConfirmada() {
        service.transicionar(transferencia, EstadoTransferencia.CONFIRMADA);
        assertThat(transferencia.getEstado()).isEqualTo(EstadoTransferencia.CONFIRMADA);
    }

    @Test
    @DisplayName("CRIADA → FALHADA: válido")
    void criadaParaFalhada() {
        service.transicionar(transferencia, EstadoTransferencia.FALHADA);
        assertThat(transferencia.getEstado()).isEqualTo(EstadoTransferencia.FALHADA);
    }

    @Test
    @DisplayName("CONFIRMADA → ESTORNADA: válido")
    void confirmadaParaEstornada() {
        transferencia.setEstado(EstadoTransferencia.CONFIRMADA);
        service.transicionar(transferencia, EstadoTransferencia.ESTORNADA);
        assertThat(transferencia.getEstado()).isEqualTo(EstadoTransferencia.ESTORNADA);
    }

    @Test
    @DisplayName("FALHADA → CONFIRMADA: inválido")
    void falhadaParaConfirmadaDeveRejeitar() {
        transferencia.setEstado(EstadoTransferencia.FALHADA);
        assertThatThrownBy(() -> service.transicionar(transferencia, EstadoTransferencia.CONFIRMADA))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Transição inválida");
    }

    @Test
    @DisplayName("ESTORNADA → CONFIRMADA: inválido (terminal)")
    void estornadaParaConfirmadaDeveRejeitar() {
        transferencia.setEstado(EstadoTransferencia.ESTORNADA);
        assertThatThrownBy(() -> service.transicionar(transferencia, EstadoTransferencia.CONFIRMADA))
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("CONFIRMADA → FALHADA: inválido")
    void confirmadaParaFalhadaDeveRejeitar() {
        transferencia.setEstado(EstadoTransferencia.CONFIRMADA);
        assertThatThrownBy(() -> service.transicionar(transferencia, EstadoTransferencia.FALHADA))
            .isInstanceOf(ApiException.class);
    }
}
