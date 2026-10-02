package com.example.demo.service;

import com.example.demo.dto.transferencia.TransferenciaResponse;
import com.example.demo.enums.EstadoTransferencia;
import com.example.demo.enums.TipoMovimento;
import com.example.demo.model.Conta;
import com.example.demo.model.Idempotencia;
import com.example.demo.model.Transferencia;
import com.example.demo.repository.ContaRepository;
import com.example.demo.repository.TransferenciaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferenciaServiceTest {

    @Mock
    private TransferenciaRepository transferenciaRepository;

    @Mock
    private IdempotenciaService idempotenciaService;

    @Mock
    private MovimentoService movimentoService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private TaxaService taxaService;

    @InjectMocks
    private TransferenciaService transferenciaService;

    private Conta origem;
    private Conta destino;
    private Conta taxas;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        origem = mock(Conta.class);
        destino = mock(Conta.class);
        taxas = mock(Conta.class);

        when(origem.getId()).thenReturn(1L);
        when(destino.getId()).thenReturn(2L);
        when(taxas.getId()).thenReturn(3L);

        when(origem.getSaldoCentavos()).thenReturn(100_000L);
        when(origem.getLimiteDiarioCentavos()).thenReturn(100_000L);
        when(destino.getSaldoCentavos()).thenReturn(20_000L);
        when(taxas.getSaldoCentavos()).thenReturn(0L);

        when(contaRepository.findByNumero("SISTEMA-TAXAS"))
                .thenReturn(Optional.of(taxas));

        when(contaRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(origem));

        when(contaRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(destino));

        when(contaRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(taxas));

        when(transferenciaRepository.somarTransferenciasDoDia(
                anyLong(),
                eq(EstadoTransferencia.CONFIRMADA),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(0L);
    }

    @Test
    void deveRealizarTransferenciaComSucesso() throws Exception {

        when(idempotenciaService.registrarSeNaoExistir(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(true);

        when(taxaService.calcularTaxa(10_000))
                .thenReturn(0L);

        when(transferenciaRepository.save(any(Transferencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}");

        TransferenciaResponse resultado =
                transferenciaService.realizarTransferencia(
                        1L,
                        2L,
                        10_000L,
                        "POST:/transferencias",
                        "teste-001",
                        "hash-001"
                );

        assertNotNull(resultado);

        verify(transferenciaRepository)
                .save(any(Transferencia.class));

        verify(movimentoService).criarMovimento(
                eq(origem),
                any(Transferencia.class),
                eq(TipoMovimento.SAIDA),
                eq(10_000L)
        );

        verify(movimentoService).criarMovimento(
                eq(destino),
                any(Transferencia.class),
                eq(TipoMovimento.ENTRADA),
                eq(10_000L)
        );

        verify(idempotenciaService).salvarResposta(
                eq("POST:/transferencias"),
                eq("teste-001"),
                eq("{}"),
                eq(201)
        );
    }

    @Test
    void deveRejeitarValorMenorOuIgualAZero() {

        when(idempotenciaService.registrarSeNaoExistir(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transferenciaService.realizarTransferencia(
                                1L,
                                2L,
                                0L,
                                "POST:/transferencias",
                                "teste-002",
                                "hash-002"
                        )
                );

        assertEquals(
                "Valor de transferencia precisa ser maior que zero",
                exception.getMessage()
        );

        verifyNoInteractions(contaRepository);
    }

    @Test
    void deveRejeitarTransferenciaParaMesmaConta() {

        when(idempotenciaService.registrarSeNaoExistir(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transferenciaService.realizarTransferencia(
                                1L,
                                1L,
                                10_000L,
                                "POST:/transferencias",
                                "teste-003",
                                "hash-003"
                        )
                );

        assertEquals(
                "Conta de origem e destino precisam ser diferentes",
                exception.getMessage()
        );

        verifyNoInteractions(contaRepository);
    }

    @Test
    void deveRejeitarQuandoLimiteDiarioForExcedido() {

        when(idempotenciaService.registrarSeNaoExistir(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(true);

        when(transferenciaRepository.somarTransferenciasDoDia(
                eq(1L),
                eq(EstadoTransferencia.CONFIRMADA),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(95_000L);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transferenciaService.realizarTransferencia(
                                1L,
                                2L,
                                10_000L,
                                "POST:/transferencias",
                                "teste-004",
                                "hash-004"
                        )
                );

        assertEquals(
                "Limite diário de transferência excedido",
                exception.getMessage()
        );

        verify(transferenciaRepository, never())
                .save(any(Transferencia.class));

        verifyNoInteractions(taxaService);
        verifyNoInteractions(movimentoService);
    }

    @Test
    void deveRejeitarQuandoSaldoForInsuficiente() {

        when(idempotenciaService.registrarSeNaoExistir(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(true);

        when(origem.getSaldoCentavos())
                .thenReturn(5_000L);

        when(taxaService.calcularTaxa(10_000))
                .thenReturn(0L);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transferenciaService.realizarTransferencia(
                                1L,
                                2L,
                                10_000L,
                                "POST:/transferencias",
                                "teste-005",
                                "hash-005"
                        )
                );

        assertEquals(
                "Saldo insuficiente",
                exception.getMessage()
        );

        verify(transferenciaRepository, never())
                .save(any(Transferencia.class));

        verifyNoInteractions(movimentoService);
    }

    @Test
    void deveAplicarTaxaNaTransferencia() throws Exception {

        when(idempotenciaService.registrarSeNaoExistir(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(true);

        when(taxaService.calcularTaxa(20_000))
                .thenReturn(200L);

        when(transferenciaRepository.save(any(Transferencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}");

        TransferenciaResponse resultado =
                transferenciaService.realizarTransferencia(
                        1L,
                        2L,
                        20_000L,
                        "POST:/transferencias",
                        "teste-006",
                        "hash-006"
                );

        assertNotNull(resultado);

        verify(movimentoService, times(2))
                .criarMovimento(
                        eq(origem),
                        any(Transferencia.class),
                        eq(TipoMovimento.SAIDA),
                        anyLong()
                );

        verify(movimentoService).criarMovimento(
                eq(taxas),
                any(Transferencia.class),
                eq(TipoMovimento.ENTRADA),
                eq(200L)
        );

        verify(transferenciaRepository)
                .save(any(Transferencia.class));

        verify(idempotenciaService).salvarResposta(
                eq("POST:/transferencias"),
                eq("teste-006"),
                eq("{}"),
                eq(201)
        );
    }

    @Test
    void deveRetornarRespostaAnteriorQuandoIdempotenciaJaExiste()
            throws Exception {

        when(idempotenciaService.registrarSeNaoExistir(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(false);

        Idempotencia existente = mock(Idempotencia.class);

        when(existente.getHashRequisicao())
                .thenReturn("hash-007");

        when(existente.getRespostaJson())
                .thenReturn("{}");

        when(idempotenciaService.buscarOuNull(
                "POST:/transferencias",
                "teste-007"
        )).thenReturn(existente);

        TransferenciaResponse respostaEsperada =
                mock(TransferenciaResponse.class);

        when(objectMapper.readValue(
                "{}",
                TransferenciaResponse.class
        )).thenReturn(respostaEsperada);

        TransferenciaResponse resultado =
                transferenciaService.realizarTransferencia(
                        1L,
                        2L,
                        10_000L,
                        "POST:/transferencias",
                        "teste-007",
                        "hash-007"
                );

        assertSame(respostaEsperada, resultado);

        verifyNoInteractions(contaRepository);
        verifyNoInteractions(transferenciaRepository);
        verifyNoInteractions(movimentoService);
        verifyNoInteractions(taxaService);
    }

    @Test
    void deveRejeitarMesmaChaveComHashDiferente() {

        when(idempotenciaService.registrarSeNaoExistir(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(false);

        Idempotencia existente = mock(Idempotencia.class);

        when(existente.getHashRequisicao())
                .thenReturn("hash-original");

        when(idempotenciaService.buscarOuNull(
                "POST:/transferencias",
                "teste-008"
        )).thenReturn(existente);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transferenciaService.realizarTransferencia(
                                1L,
                                2L,
                                20_000L,
                                "POST:/transferencias",
                                "teste-008",
                                "hash-diferente"
                        )
                );

        assertEquals(
                "Idempotency-Key já utilizada com outra requisição",
                exception.getMessage()
        );

        verifyNoInteractions(contaRepository);
        verifyNoInteractions(transferenciaRepository);
        verifyNoInteractions(movimentoService);
        verifyNoInteractions(taxaService);
    }
}