package com.example.demo.service;

import com.example.demo.model.Idempotencia;
import com.example.demo.repository.IdempotenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdempotenciaServiceTest {

    @Mock
    private IdempotenciaRepository idempotenciaRepository;

    @InjectMocks
    private IdempotenciaService idempotenciaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deveGerarMesmoHashParaMesmaRequisicao() {

        String requisicao = """
                {
                    "contaOrigemId": 3,
                    "contaDestinoId": 4,
                    "valorCentavos": 10000
                }
                """;

        String hash1 = idempotenciaService.calcularHash(requisicao);
        String hash2 = idempotenciaService.calcularHash(requisicao);

        assertEquals(hash1, hash2);
    }

    @Test
    void deveEncontrarIdempotenciaExistente() {

        String endpoint = "POST:/transferencias";
        String chave = "transferencia-001";

        Idempotencia idempotencia = new Idempotencia();

        when(idempotenciaRepository.findByEndpointAndChave(
                endpoint,
                chave
        )).thenReturn(Optional.of(idempotencia));

        Idempotencia resultado = idempotenciaService.buscarOuNull(endpoint, chave);

        assertSame(idempotencia, resultado);

        verify(idempotenciaRepository).findByEndpointAndChave(endpoint, chave);
    }

    @Test
    void deveRetornarNullQuandoIdempotenciaNaoExiste() {

        String endpoint = "POST:/transferencias";
        String chave = "transferencia-001";

        when(idempotenciaRepository.findByEndpointAndChave(
                endpoint,
                chave
        )).thenReturn(Optional.empty());

        Idempotencia resultado = idempotenciaService.buscarOuNull(endpoint, chave);

        assertNull(resultado);

        verify(idempotenciaRepository).findByEndpointAndChave(endpoint, chave);
    }

    @Test
    void deveRetornarTrueQuandoIdempotenciaFoiCriada() {

        when(idempotenciaRepository.criarSeNaoExistir(
                "transferencia-001",
                "POST:/transferencias",
                "abc123"
        )).thenReturn(1);

        boolean resultado =
                idempotenciaService.registrarSeNaoExistir(
                        "POST:/transferencias",
                        "transferencia-001",
                        "abc123"
                );

        assertTrue(resultado);

        verify(idempotenciaRepository).criarSeNaoExistir(
                "transferencia-001",
                "POST:/transferencias",
                "abc123"
        );
    }

    @Test
    void deveRetornarFalseQuandoIdempotenciaJaExiste() {

        when(idempotenciaRepository.criarSeNaoExistir(
                "transferencia-001",
                "POST:/transferencias",
                "abc123"
        )).thenReturn(0);

        boolean resultado = idempotenciaService.registrarSeNaoExistir(
                        "POST:/transferencias",
                        "transferencia-001",
                        "abc123"
                );

        assertFalse(resultado);

        verify(idempotenciaRepository).criarSeNaoExistir(
                "transferencia-001",
                "POST:/transferencias",
                "abc123"
        );
    }

    @Test
    void deveSalvarRespostaDaIdempotencia() {

        String endpoint = "POST:/transferencias";
        String chave = "transferencia-001";
        String respostaJson = """
                {
                    "id": 1,
                    "estado": "CONFIRMADA"
                }
                """;

        Idempotencia idempotencia = new Idempotencia();

        when(idempotenciaRepository.findByEndpointAndChave(
                endpoint,
                chave
        )).thenReturn(Optional.of(idempotencia));

        idempotenciaService.salvarResposta(
                endpoint,
                chave,
                respostaJson,
                201
        );

        assertEquals(respostaJson, idempotencia.getRespostaJson());
        assertEquals(201, idempotencia.getStatusHttp());

        verify(idempotenciaRepository).save(idempotencia);
    }

    @Test
    void deveLancarExcecaoAoSalvarRespostaDeIdempotenciaInexistente() {

        String endpoint = "POST:/transferencias";
        String chave = "transferencia-001";

        when(idempotenciaRepository.findByEndpointAndChave(
                endpoint,
                chave
        )).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> idempotenciaService.salvarResposta(
                        endpoint,
                        chave,
                        "{}",
                        201
                )
        );

        verify(idempotenciaRepository, never()).save(any());
    }
}