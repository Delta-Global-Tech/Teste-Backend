package com.financial.transfer.integration;

import com.financial.transfer.dto.request.DepositoRequest;
import com.financial.transfer.repository.ContaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Depósito — testes de integração")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepositoIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired ContaRepository  contaRepository;

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
    @DisplayName("201 — depósito em conta ativa")
    void depositoEmContaAtiva() {
        var req = new DepositoRequest(new BigDecimal("250.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity(
            "/contas/CONTA-001/depositos", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).contains("saldoApos");
    }

    @Test
    @Order(2)
    @DisplayName("201 — depósito em conta bloqueada deve ser aceito")
    void depositoEmContaBloqueada() {
        var req = new DepositoRequest(new BigDecimal("100.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity(
            "/contas/CONTA-006/depositos", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @Order(3)
    @DisplayName("409 — depósito em conta encerrada deve ser rejeitado")
    void depositoEmContaEncerrada() {
        var req = new DepositoRequest(new BigDecimal("100.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity(
            "/contas/CONTA-007/depositos", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("CONTA_DESTINO_ENCERRADA");
    }

    @Test
    @Order(4)
    @DisplayName("404 — conta inexistente")
    void contaInexistente() {
        var req = new DepositoRequest(new BigDecimal("100.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity(
            "/contas/CONTA-999/depositos", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(5)
    @DisplayName("422 — valor zero deve ser rejeitado")
    void valorZero() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Idempotency-Key", UUID.randomUUID().toString());
        var entity = new HttpEntity<>("{\"valor\": 0}", h);

        ResponseEntity<String> resp = restTemplate.postForEntity(
            "/contas/CONTA-001/depositos", entity, String.class);

        assertThat(resp.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Order(6)
    @DisplayName("422 — valor acima de R$ 10.000,00 deve ser rejeitado")
    void valorAcimaDoMaximo() {
        var req = new DepositoRequest(new BigDecimal("10001.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity(
            "/contas/CONTA-001/depositos", entity, String.class);

        assertThat(resp.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Order(7)
    @DisplayName("400 — header Idempotency-Key ausente")
    void headerAusente() {
        var req = new DepositoRequest(new BigDecimal("100.00"));
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>(req, h);

        ResponseEntity<String> resp = restTemplate.postForEntity(
            "/contas/CONTA-001/depositos", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(8)
    @DisplayName("200 — segunda requisição com mesma Idempotency-Key retorna resposta original")
    void idempotencia() {
        String key = UUID.randomUUID().toString();
        var req = new DepositoRequest(new BigDecimal("150.00"));
        var entity1 = new HttpEntity<>(req, headers(key));
        var entity2 = new HttpEntity<>(req, headers(key));

        ResponseEntity<String> resp1 = restTemplate.postForEntity("/contas/CONTA-001/depositos", entity1, String.class);
        ResponseEntity<String> resp2 = restTemplate.postForEntity("/contas/CONTA-001/depositos", entity2, String.class);

        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp2.getStatusCode().value()).isIn(200, 201);
        // O saldo da conta deve ter aumentado apenas uma vez
    }

    @Test
    @Order(9)
    @DisplayName("409 — mesma Idempotency-Key com corpo diferente deve ser rejeitado")
    void idempotenciaCorpoDiferente() {
        String key = UUID.randomUUID().toString();

        var entity1 = new HttpEntity<>(new DepositoRequest(new BigDecimal("100.00")), headers(key));
        var entity2 = new HttpEntity<>(new DepositoRequest(new BigDecimal("200.00")), headers(key));

        restTemplate.postForEntity("/contas/CONTA-001/depositos", entity1, String.class);
        ResponseEntity<String> resp2 = restTemplate.postForEntity("/contas/CONTA-001/depositos", entity2, String.class);

        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp2.getBody()).contains("IDEMPOTENCIA_CONFLITO");
    }
}
