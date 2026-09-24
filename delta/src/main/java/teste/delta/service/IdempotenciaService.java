package teste.delta.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import teste.delta.dto.response.RespostaIdempotente;
import teste.delta.exception.ApiException;
import teste.delta.model.Idempotencia;
import teste.delta.repository.IdempotenciaRepository;

/**
 * IdempotenciaService
 */
@Service
public class IdempotenciaService {

    @Autowired
    IdempotenciaRepository idempotenciaRepository;

    @Autowired
    ObjectMapper objectMapper;

    /**
     * Executa a operação uma única vez por (chave, endpoint), na mesma transação que grava a chave.
     * Se a operação lançar exceção, o rollback desfaz também a reserva da chave.
     */
    @Transactional
    public RespostaIdempotente executar(
        String chave,
        String endpoint,
        String hashRequisicao,
        HttpStatus statusSucesso,
        Supplier<?> operacao
    ){
        int reservadas = idempotenciaRepository.reservar(chave, endpoint, hashRequisicao);
        if (reservadas == 0) {
            Idempotencia existente = idempotenciaRepository.findByChaveAndEndpoint(chave, endpoint).orElseThrow();
            if (!existente.getHashRequisicao().equals(hashRequisicao)) {
                throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_REUTILIZADA",
                    "Idempotency-Key já utilizada com um corpo de requisição diferente"
                );
            }
            return new RespostaIdempotente(HttpStatus.OK.value(), existente.getRespostaJson());
        }

        String json = this.serializar(operacao.get());
        idempotenciaRepository.registrarResposta(chave, endpoint, json, statusSucesso.value());
        return new RespostaIdempotente(statusSucesso.value(), json);
    }

    public String hash(String conteudo){
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String serializar(Object resposta){
        try {
            return objectMapper.writeValueAsString(resposta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar resposta", e);
        }
    }
}
