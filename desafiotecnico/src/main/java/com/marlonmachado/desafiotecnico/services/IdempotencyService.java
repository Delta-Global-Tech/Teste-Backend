package com.marlonmachado.desafiotecnico.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlonmachado.desafiotecnico.entities.IdempotencyRecord;
import com.marlonmachado.desafiotecnico.exceptions.BusinessException;
import com.marlonmachado.desafiotecnico.repositories.IdempotencyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository idempotencyRecordRepository, ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<IdempotencyRecord> buscarRegistroExistente(String endpoint, String chave, Object corpoRequisicao) {
        Optional<IdempotencyRecord> registro =
                idempotencyRecordRepository.findByEndpointAndKey(endpoint, chave);

        if (registro.isEmpty()) {
            return Optional.empty();
        }

        String corpoAtualJson = calcularHash(paraJson(corpoRequisicao));

        if (!registro.get().getRequestHash().equals(corpoAtualJson)) {
            throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLITANTE",
                    "A chave de idempotência já foi usada com um corpo de requisição diferente");
        }

        return registro;
    }

    public void salvarRegistro(String endpoint, String chave, Object corpoRequisicao, Object resposta, int statusHttp) {
        String hash = calcularHash(paraJson(corpoRequisicao));

        IdempotencyRecord novoRegistro = new IdempotencyRecord(
                null, chave, endpoint, hash, paraJson(resposta), statusHttp, Instant.now());

        idempotencyRecordRepository.save(novoRegistro);
    }

    public <T> T lerResposta(IdempotencyRecord registro, Class<T> tipo) {
        try {
            return objectMapper.readValue(registro.getResponseJson(), tipo);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao ler resposta armazenada da idempotência", e);
        }
    }

    private String paraJson(Object objeto) {
        try {
            return objectMapper.writeValueAsString(objeto);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao converter objeto para JSON", e);
        }
    }

    private String calcularHash(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesHash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytesHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível", e);
        }
    }
}