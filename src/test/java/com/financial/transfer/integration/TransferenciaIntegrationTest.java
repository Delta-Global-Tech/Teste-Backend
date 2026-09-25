package com.financial.transfer.integration;

import com.financial.transfer.dto.request.EstornoRequest;
import com.financial.transfer.dto.request.TransferenciaRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transferência e Estorno — testes de integração")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransferenciaIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper     objectMapper;

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
    @DisplayName("201 — transferência básica com taxa")
    void transferenciaBásica() throws Exception {
        var req = new TransferenciaRequest("CONTA-001", "CONTA-002", new BigDecimal("150.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity("/transferencias", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(resp.getBody());
        assertThat(body.get("taxa").decimalValue()).isEqualByComparingTo("1.50");
        assertThat(body.get("totalDebitado").decimalValue()).isEqualByComparingTo("151.50");
        assertThat(body.get("saldoOrigemApos").decimalValue()).isEqualByComparingTo("848.50");
    }

    @Test
    @Order(2)
    @DisplayName("422 — saldo insuficiente")
    void saldoInsuficiente() {
        // CONTA-001 tem R$ 1.000,00; tenta transferir R$ 2.000,00
        var req = new TransferenciaRequest("CONTA-001", "CONTA-002", new BigDecimal("2000.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity("/transferencias", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).contains("SALDO_INSUFICIENTE");
    }

    @Test
    @Order(3)
    @DisplayName("422 — limite diário excedido")
    void limiteDiarioExcedido() {
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        var req1 = new TransferenciaRequest("CONTA-001", "CONTA-002", new BigDecimal("1100.00"));

        restTemplate.postForEntity("/transferencias",
            new HttpEntity<>(req1, headers(key1)), String.class);

       
        var req2 = new TransferenciaRequest("CONTA-001", "CONTA-003", new BigDecimal("1100.00"));
        ResponseEntity<String> resp2 = restTemplate.postForEntity("/transferencias",
            new HttpEntity<>(req2, headers(key2)), String.class);

        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Order(4)
    @DisplayName("409 — conta origem bloqueada")
    void contaOrigemBloqueada() {
        var req = new TransferenciaRequest("CONTA-006", "CONTA-001", new BigDecimal("100.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity("/transferencias", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("CONTA_BLOQUEADA");
    }

    @Test
    @Order(5)
    @DisplayName("409 — conta origem encerrada")
    void contaOrigemEncerrada() {
        var req = new TransferenciaRequest("CONTA-007", "CONTA-001", new BigDecimal("100.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity("/transferencias", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("CONTA_ENCERRADA");
    }

    @Test
    @Order(6)
    @DisplayName("409 — conta destino encerrada")
    void contaDestinoEncerrada() {
        var req = new TransferenciaRequest("CONTA-001", "CONTA-007", new BigDecimal("100.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity("/transferencias", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("CONTA_DESTINO_ENCERRADA");
    }

    @Test
    @Order(7)
    @DisplayName("409 — origem igual ao destino")
    void origemIgualDestino() {
        var req = new TransferenciaRequest("CONTA-001", "CONTA-001", new BigDecimal("100.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity("/transferencias", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("CONTAS_IGUAIS");
    }

    @Test
    @Order(8)
    @DisplayName("404 — conta origem inexistente")
    void contaOrigemInexistente() {
        var req = new TransferenciaRequest("CONTA-999", "CONTA-002", new BigDecimal("100.00"));
        var entity = new HttpEntity<>(req, headers(UUID.randomUUID().toString()));

        ResponseEntity<String> resp = restTemplate.postForEntity("/transferencias", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(9)
    @DisplayName("200 — idempotência: segunda requisição retorna mesma resposta")
    void idempotenciaTransferencia() {
        String key = UUID.randomUUID().toString();
        var req = new TransferenciaRequest("CONTA-001", "CONTA-002", new BigDecimal("100.00"));

        ResponseEntity<String> r1 = restTemplate.postForEntity("/transferencias",
            new HttpEntity<>(req, headers(key)), String.class);
        ResponseEntity<String> r2 = restTemplate.postForEntity("/transferencias",
            new HttpEntity<>(req, headers(key)), String.class);

        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r2.getStatusCode().value()).isIn(200, 201);
    }

    @Test
    @Order(10)
    @DisplayName("201 — estorno de transferência confirmada")
    void estornoDeTransferenciaConfirmada() throws Exception {
        var reqT = new TransferenciaRequest("CONTA-001", "CONTA-002", new BigDecimal("100.00"));
        ResponseEntity<String> respT = restTemplate.postForEntity("/transferencias",
            new HttpEntity<>(reqT, headers(UUID.randomUUID().toString())), String.class);
        assertThat(respT.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Long transferenciaId = objectMapper.readTree(respT.getBody()).get("transferenciaId").asLong();

       
        var reqE = new EstornoRequest("Teste de estorno");
        ResponseEntity<String> respE = restTemplate.postForEntity(
            "/transferencias/" + transferenciaId + "/estorno",
            new HttpEntity<>(reqE, headers(UUID.randomUUID().toString())),
            String.class);

        assertThat(respE.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respE.getBody()).contains("transferenciaEstornoId");
    }

    @Test
    @Order(11)
    @DisplayName("409 — estorno de transferência já estornada")
    void estornoDeJaEstornada() throws Exception {
        var reqT = new TransferenciaRequest("CONTA-001", "CONTA-002", new BigDecimal("100.00"));
        ResponseEntity<String> respT = restTemplate.postForEntity("/transferencias",
            new HttpEntity<>(reqT, headers(UUID.randomUUID().toString())), String.class);
        Long id = objectMapper.readTree(respT.getBody()).get("transferenciaId").asLong();

        var reqE = new EstornoRequest("Primeiro estorno");
        restTemplate.postForEntity("/transferencias/" + id + "/estorno",
            new HttpEntity<>(reqE, headers(UUID.randomUUID().toString())), String.class);

       
        ResponseEntity<String> resp2 = restTemplate.postForEntity("/transferencias/" + id + "/estorno",
            new HttpEntity<>(new EstornoRequest("Segundo estorno"), headers(UUID.randomUUID().toString())),
            String.class);

        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp2.getBody()).contains("TRANSFERENCIA_JA_ESTORNADA");
    }
}
