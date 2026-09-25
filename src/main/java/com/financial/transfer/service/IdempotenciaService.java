package com.financial.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.transfer.entity.Idempotencia;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.repository.IdempotenciaRepository;
import com.financial.transfer.util.HashUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotenciaService {

    private final IdempotenciaRepository idempotenciaRepository;
    private final ObjectMapper objectMapper;

    public IdempotenciaService(IdempotenciaRepository idempotenciaRepository,
            ObjectMapper objectMapper) {
        this.idempotenciaRepository = idempotenciaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void adquirirLockTransacional(String endpoint, String chave) {
        String chaveLock = endpoint + ":" + chave;
        idempotenciaRepository.adquirirLockTransacional(chaveLock);
    }

    public Optional<IdempotenciaResult> verificar(String endpoint, String chave, String bodyJson) {
        String hash = HashUtil.sha256(bodyJson);

        Optional<Idempotencia> existing = idempotenciaRepository.findByEndpointAndChave(endpoint, chave);
        if (existing.isPresent()) {
            Idempotencia reg = existing.get();
            if (!reg.getHashRequisicao().equals(hash)) {
                throw ApiException.idempotenciaConflito();
            }
            return Optional.of(new IdempotenciaResult(reg.getRespostaJson(), reg.getStatusHttp()));
        }

        return Optional.empty();
    }

    public void registrar(String endpoint, String chave, String bodyJson,
            Object resposta, int statusHttp) {
        String hash = HashUtil.sha256(bodyJson);
        String respostaJson;
        try {
            respostaJson = objectMapper.writeValueAsString(resposta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar resposta para idempotência", e);
        }

        Idempotencia reg = new Idempotencia();
        reg.setChave(chave);
        reg.setEndpoint(endpoint);
        reg.setHashRequisicao(hash);
        reg.setRespostaJson(respostaJson);
        reg.setStatusHttp(statusHttp);

        try {
            idempotenciaRepository.saveAndFlush(reg);
        } catch (DataIntegrityViolationException e) {
            // Ler o que a outra inseriu e verificar se o corpo bate
            Idempotencia existente = idempotenciaRepository
                    .findByEndpointAndChave(endpoint, chave)
                    .orElseThrow(() -> new IllegalStateException("Registro de idempotência desapareceu", e));
            if (!existente.getHashRequisicao().equals(hash)) {
                throw ApiException.idempotenciaConflito();
            }
            throw ApiException.conflitoConcorrencia();
        }
    }

    public <T> T deserializar(String json, Class<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao desserializar resposta de idempotência", e);
        }
    }

    public record IdempotenciaResult(String json, int statusHttp) {

    }
}
