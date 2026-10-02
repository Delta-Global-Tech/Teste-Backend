package com.example.demo.service;

import com.example.demo.model.Idempotencia;
import com.example.demo.repository.IdempotenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class IdempotenciaService {

    @Autowired
    private IdempotenciaRepository idempotenciaRepository;

    public Idempotencia buscarOuNull(
            String endpoint,
            String chave
    ) {
        return idempotenciaRepository
                .findByEndpointAndChave(endpoint, chave)
                .orElse(null);
    }

    public boolean registrarSeNaoExistir(
            String endpoint,
            String chave,
            String hash
    ) {
        return idempotenciaRepository.criarSeNaoExistir(
                chave,
                endpoint,
                hash
        ) == 1; // comparação com o retorno int para gerar boolean
    }

    public void salvarResposta(
            String endpoint,
            String chave,
            String respostaJson,
            int statusHttp
    ) {
        Idempotencia idempotencia = idempotenciaRepository
                        .findByEndpointAndChave(endpoint, chave)
                        .orElseThrow();

        idempotencia.setRespostaJson(respostaJson);
        idempotencia.setStatusHttp(statusHttp);

        idempotenciaRepository.save(idempotencia);
    }

    public String calcularHash(String requisicao) {
        try {

            // Objeto que calcular hash usando SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Transforma a requisicao em byte (formato que SHA-256 trabalha)
            byte[] hash = digest.digest(
                    requisicao.getBytes(StandardCharsets.UTF_8)
            );


            StringBuilder resultado = new StringBuilder();


            for (byte b : hash) {

                // Converte cada byte para hexadecimal com dois caracteres
                resultado.append(String.format("%02x", b));
            }

            // Retorna o hash completo em formato hexadecimal
            return resultado.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não esta disponível", e);
        }
    }
}