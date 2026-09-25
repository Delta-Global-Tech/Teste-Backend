package com.financial.transfer.unit;

import com.financial.transfer.entity.Conta;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.repository.TransferenciaRepository;
import com.financial.transfer.service.LimiteDiarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LimiteDiarioService — verificação de limite diário")
class LimiteDiarioServiceTest {

    @Mock
    private TransferenciaRepository transferenciaRepository;

    private LimiteDiarioService service;
    private Conta conta;

    @BeforeEach
    void setUp() {
        service = new LimiteDiarioService(transferenciaRepository);
        conta = new Conta();
        conta.setLimiteDiarioCentavos(200_000L); // R$ 2.000,00
    }

    @Test
    @DisplayName("Transferência dentro do limite disponível deve ser aceita")
    void dentroDoLimiteDeveAceitar() {
        // Já usou R$ 500,00 hoje, tenta mais R$ 1.000,00 — total R$ 1.500,00 < R$ 2.000,00
        when(transferenciaRepository.somaTransferidaHoje(any(), any(), any()))
            .thenReturn(50_000L);

        assertThatCode(() -> service.validar(conta, 100_000L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Transferência que ultrapassa o limite deve ser rejeitada")
    void ultrapassaLimiteDeveRejeitar() {
        // Já usou R$ 1.800,00, tenta mais R$ 300,00 — total R$ 2.100,00 > R$ 2.000,00
        when(transferenciaRepository.somaTransferidaHoje(any(), any(), any()))
            .thenReturn(180_000L);

        assertThatThrownBy(() -> service.validar(conta, 30_000L))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Limite diário");
    }

    @Test
    @DisplayName("Transferência que usa exatamente o limite restante deve ser aceita")
    void usaExatamenteOLimiteRestante() {
        // Já usou R$ 1.000,00, tenta mais R$ 1.000,00 — total = R$ 2.000,00 (exato)
        when(transferenciaRepository.somaTransferidaHoje(any(), any(), any()))
            .thenReturn(100_000L);

        assertThatCode(() -> service.validar(conta, 100_000L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sem histórico hoje: limite total disponível")
    void semHistoricoHojeLimiteTotalDisponivel() {
        when(transferenciaRepository.somaTransferidaHoje(any(), any(), any()))
            .thenReturn(0L);

        assertThatCode(() -> service.validar(conta, 200_000L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Já atingiu o limite: qualquer valor deve ser rejeitado")
    void jaAtingiuLimiteDeveRejeitar() {
        when(transferenciaRepository.somaTransferidaHoje(any(), any(), any()))
            .thenReturn(200_000L);

        assertThatThrownBy(() -> service.validar(conta, 1L))
            .isInstanceOf(ApiException.class);
    }
}
