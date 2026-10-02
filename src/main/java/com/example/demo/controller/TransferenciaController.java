package com.example.demo.controller;

import com.example.demo.dto.estorno.EstornoRequest;
import com.example.demo.dto.transferencia.TransferenciaRequest;
import com.example.demo.dto.transferencia.TransferenciaResponse;

import com.example.demo.model.Idempotencia;

import com.example.demo.service.EstornoService;
import com.example.demo.service.IdempotenciaService;
import com.example.demo.service.TransferenciaService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    @Autowired
    private IdempotenciaService idempotenciaService;

    @Autowired
    private TransferenciaService transferenciaService;

    @Autowired
    private EstornoService estornoService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<TransferenciaResponse> realizarTransferencia(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody TransferenciaRequest request
    ) throws JsonProcessingException {

        String endpoint = "POST:/transferencias";

        String requestJson = objectMapper.writeValueAsString(request);

        String hash = idempotenciaService.calcularHash(requestJson);

        Idempotencia existing = idempotenciaService.buscarOuNull(endpoint, key);

        if (existing != null) {

            if (!existing.getHashRequisicao().equals(hash)) {

                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            try {
                TransferenciaResponse response = objectMapper.readValue(
                        existing.getRespostaJson(),
                        TransferenciaResponse.class
                );

                return ResponseEntity.status(HttpStatus.OK).body(response);

            } catch (JsonProcessingException e) {

                throw new IllegalStateException(
                        "Não foi possível recuperar a resposta da idempotência", e
                );
            }
        }

        TransferenciaResponse response = transferenciaService.realizarTransferencia(
                        request.getContaOrigemId(),
                        request.getContaDestinoId(),
                        request.getValorCentavos(),
                        endpoint,
                        key,
                        hash
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/estorno")
    public ResponseEntity<TransferenciaResponse> estornar(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EstornoRequest request
    ) throws JsonProcessingException {

        String endpoint = "POST:/transferencias/" + id + "/estorno";

        String requisicaoJson = objectMapper.writeValueAsString(request);

        String hash = idempotenciaService.calcularHash(requisicaoJson);

        Idempotencia existente = idempotenciaService.buscarOuNull(
                        endpoint,
                        idempotencyKey
                );

        if (existente != null) {

            if (!existente.getHashRequisicao().equals(hash)) {

                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            try {
                TransferenciaResponse response = objectMapper.readValue(
                        existente.getRespostaJson(),
                        TransferenciaResponse.class
                );

                return ResponseEntity.status(HttpStatus.OK).body(response);

            } catch (JsonProcessingException e) {

                throw new IllegalStateException("Não foi possível recuperar a resposta da idempotência", e);
            }
        }

        TransferenciaResponse response = estornoService.estornarTransferencia(
                        id,
                        endpoint,
                        idempotencyKey,
                        hash
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}