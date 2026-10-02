package com.example.demo.controller;

import com.example.demo.dto.conta.ContaResponse;
import com.example.demo.dto.deposito.DepositoRequest;

import com.example.demo.model.Idempotencia;

import com.example.demo.service.ContaService;
import com.example.demo.service.DepositoService;
import com.example.demo.service.IdempotenciaService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contas")
public class ContaController {

    @Autowired
    DepositoService depositoService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    IdempotenciaService idempotenciaService;

    @PostMapping("/{numero}/depositos")
    public ResponseEntity<ContaResponse> depositar(
            @PathVariable String numero,
            @RequestHeader("Idempotency-Key") String chave,
            @Valid @RequestBody DepositoRequest request
    ) throws JsonProcessingException {

        String endpoint = "POST:/contas/{numero}/depositos";

        String requisicaoJson = objectMapper.writeValueAsString(request);

        String hash = idempotenciaService.calcularHash(requisicaoJson);

        Idempotencia existente = idempotenciaService.buscarOuNull(endpoint, chave);

        if (existente != null) {

            if (!existente.getHashRequisicao().equals(hash)) {

                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            ContaResponse response = objectMapper.readValue(
                            existente.getRespostaJson(),
                            ContaResponse.class
                    );

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        ContaResponse response = depositoService.realizarDeposito(
                        numero,
                        request.getValorCentavos(),
                        endpoint,
                        chave,
                        hash
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
