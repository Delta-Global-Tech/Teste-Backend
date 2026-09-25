package com.financial.transfer.concurrency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.transfer.dto.request.EstornoRequest;
import com.financial.transfer.dto.request.TransferenciaRequest;
import com.financial.transfer.repository.ContaRepository;
import com.financial.transfer.repository.MovimentoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Concorrência — transferências simultâneas")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransferenciaConcurrenteTest extends ConcurrencyTestBase {

    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    ContaRepository contaRepository;
    @Autowired
    MovimentoRepository movimentoRepository;
    @Autowired
    ObjectMapper objectMapper;

    private HttpHeaders headers(String key) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Idempotency-Key", key);
        return h;
    }

    @BeforeEach
    void resetar() {
        restTemplate.delete("/dados");
    }

    @Test
    @Order(1)
    @DisplayName("Cenário 1: duas transferências simultâneas — saldo para apenas uma")
    void cenario1_duasTransferenciasSaldoParaUma() throws Exception {
        var results = executarConcorrentemente(2, idx -> {
            var req = new TransferenciaRequest(
                    "CONTA-001",
                    "CONTA-00" + (idx + 2),
                    new BigDecimal("700.00")
            );

            return restTemplate.postForEntity(
                    "/transferencias",
                    new HttpEntity<>(req, headers(UUID.randomUUID().toString())),
                    String.class
            );
        });

        results.forEach(r -> {
            if (r.result() != null) {
                System.out.println(
                        "THREAD " + r.threadIndex()
                        + " -> HTTP " + r.result().getStatusCode()
                        + " -> " + r.result().getBody()
                );
            } else {
                System.out.println(
                        "THREAD " + r.threadIndex()
                        + " -> EXCEPTION: " + r.error()
                );
            }
        });

        long confirmadas = results.stream()
                .filter(r -> r.result() != null)
                .filter(r -> r.result().getStatusCode() == HttpStatus.CREATED)
                .count();

        long rejeitadas = results.stream()
                .filter(r -> r.result() != null)
                .filter(r -> r.result().getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY
                || r.result().getStatusCode() == HttpStatus.CONFLICT)
                .count();

        assertThat(confirmadas)
                .as("Exatamente uma transferência deve ser confirmada")
                .isEqualTo(1);

        assertThat(rejeitadas)
                .as("Exatamente uma transferência deve ser rejeitada")
                .isEqualTo(1);

        verificarConsistencia("CONTA-001");
    }

    @Test
    @Order(2)
    @DisplayName("Cenário 2: duas transferências simultâneas que juntas ultrapassam o limite diário")
    void cenario2_limiteDiarioSimultaneo() throws Exception {

        // Aumenta o saldo da conta para que ambas as transferências
        // tenham saldo suficiente individualmente.
        var deposito = new com.financial.transfer.dto.request.DepositoRequest(
                new BigDecimal("2500.00")
        );

        ResponseEntity<String> depositoResponse = restTemplate.postForEntity(
                "/contas/CONTA-001/depositos",
                new HttpEntity<>(deposito, headers(UUID.randomUUID().toString())),
                String.class
        );

        assertThat(depositoResponse.getStatusCode())
                .as("O depósito de preparação deve ser realizado com sucesso")
                .isEqualTo(HttpStatus.CREATED);

        var results = executarConcorrentemente(2, idx -> {

            var req = new TransferenciaRequest(
                    "CONTA-001",
                    "CONTA-00" + (idx + 2),
                    new BigDecimal("1100.00")
            );

            return restTemplate.postForEntity(
                    "/transferencias",
                    new HttpEntity<>(
                            req,
                            headers(UUID.randomUUID().toString())
                    ),
                    String.class
            );
        });

        long confirmadas = results.stream()
                .filter(r -> r.result() != null)
                .filter(r -> r.result().getStatusCode() == HttpStatus.CREATED)
                .count();

        long rejeitadas = results.stream()
                .filter(r -> r.result() != null)
                .filter(r -> r.result().getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY)
                .count();

        assertThat(confirmadas)
                .as("Somente uma transferência deve passar no limite diário")
                .isEqualTo(1);

        assertThat(rejeitadas)
                .as("A segunda transferência deve ser rejeitada pelo limite diário")
                .isEqualTo(1);

        verificarConsistencia("CONTA-001");
    }

    @Test
    @Order(3)
    @DisplayName("Cenário 3: transferências cruzadas sem deadlock")
    void cenario3_transferenciasCruzadasSemDeadlock() throws Exception {
        var results = executarConcorrentemente(2, idx -> {
            String origem = idx == 0 ? "CONTA-001" : "CONTA-002";
            String destino = idx == 0 ? "CONTA-002" : "CONTA-001";

            var req = new TransferenciaRequest(
                    origem,
                    destino,
                    new BigDecimal("100.00")
            );

            return restTemplate.postForEntity(
                    "/transferencias",
                    new HttpEntity<>(req, headers(UUID.randomUUID().toString())),
                    String.class
            );
        });

        // DIAGNÓSTICO TEMPORÁRIO
        results.forEach(r -> {
            if (r.result() != null) {
                System.out.println(
                        "THREAD " + r.threadIndex()
                        + " -> HTTP " + r.result().getStatusCode()
                        + " -> " + r.result().getBody()
                );
            } else {
                System.out.println(
                        "THREAD " + r.threadIndex()
                        + " -> EXCEPTION: " + r.error()
                );
            }
        });

        long confirmadas = results.stream()
                .filter(r -> r.result() != null
                && r.result().getStatusCode() == HttpStatus.CREATED)
                .count();

        assertThat(confirmadas)
                .as("Ambas as transferências cruzadas devem ser confirmadas")
                .isEqualTo(2);

        verificarConsistencia("CONTA-001");
        verificarConsistencia("CONTA-002");
    }

    @Test
    @Order(4)
    @DisplayName("Cenário 4: N requisições simultâneas com mesma Idempotency-Key")
    void cenario4_idempotenciaSimultanea() throws Exception {
        String chaveUnica = UUID.randomUUID().toString();
        int nThreads = 5;

        var results = executarConcorrentemente(nThreads, idx -> {
            var req = new TransferenciaRequest("CONTA-001", "CONTA-002", new BigDecimal("50.00"));
            return restTemplate.postForEntity("/transferencias",
                    new HttpEntity<>(req, headers(chaveUnica)), String.class);
        });

        long sucessos = results.stream()
                .filter(r -> r.result() != null)
                .filter(r -> r.result().getStatusCode() == HttpStatus.CREATED
                || r.result().getStatusCode() == HttpStatus.OK)
                .count();

        assertThat(sucessos).isEqualTo(nThreads);

        ResponseEntity<String> extrato = restTemplate.getForEntity(
                "/contas/CONTA-001/extrato?size=100", String.class);
        JsonNode body = objectMapper.readTree(extrato.getBody());
        assertThat(body.get("totalMovimentos").asLong()).isLessThanOrEqualTo(5);

        verificarConsistencia("CONTA-001");
    }

    @Test
    @Order(5)
    @DisplayName("Cenário 5: dois estornos simultâneos — apenas um deve ser efetivado")
    void cenario5_doisEstornosSimultaneos() throws Exception {
        // Fazer uma transferência primeiro
        var reqT = new TransferenciaRequest("CONTA-001", "CONTA-002", new BigDecimal("100.00"));
        ResponseEntity<String> respT = restTemplate.postForEntity("/transferencias",
                new HttpEntity<>(reqT, headers(UUID.randomUUID().toString())), String.class);
        assertThat(respT.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Long transferenciaId = objectMapper.readTree(respT.getBody()).get("transferenciaId").asLong();

        var results = executarConcorrentemente(2, idx -> {
            var reqE = new EstornoRequest("Estorno concorrente " + idx);
            return restTemplate.postForEntity(
                    "/transferencias/" + transferenciaId + "/estorno",
                    new HttpEntity<>(reqE, headers(UUID.randomUUID().toString())),
                    String.class);
        });

        long confirmados = results.stream()
                .filter(r -> r.result() != null && r.result().getStatusCode() == HttpStatus.CREATED)
                .count();

        long conflitos = results.stream()
                .filter(r -> r.result() != null && r.result().getStatusCode() == HttpStatus.CONFLICT)
                .count();

        assertThat(confirmados).as("Exatamente um estorno deve ser confirmado").isEqualTo(1);
        assertThat(conflitos).as("O segundo estorno deve ser rejeitado com 409").isEqualTo(1);

        verificarConsistencia("CONTA-001");
        verificarConsistencia("CONTA-002");
    }

    private void verificarConsistencia(String numeroConta) {
        var conta = contaRepository.findByNumero(numeroConta).orElseThrow();
        Long contaId = conta.getId();

        Long countSeqs = movimentoRepository.contarSequencias(contaId);
        Long maxSeq = movimentoRepository.maxSequencia(contaId);
        assertThat(countSeqs)
                .as("Sequências de " + numeroConta + " não devem ter buracos")
                .isEqualTo(maxSeq);

        var ultimoSaldo = movimentoRepository.saldoAposUltimoMovimento(contaId);
        if (ultimoSaldo.isPresent()) {
            assertThat(conta.getSaldoCentavos())
                    .as("Saldo de " + numeroConta + " deve bater com o último movimento")
                    .isEqualTo(ultimoSaldo.get());
        }

        Long somaGlobal = movimentoRepository.somaGlobal();
        assertThat(somaGlobal)
                .as("Soma de todos os movimentos deve ser zero (princípio da partida dobrada)")
                .isZero();
    }
}
