package com.financial.transfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.transfer.concurrency.IdempotencyContext;
import com.financial.transfer.dto.request.DepositoRequest;
import com.financial.transfer.dto.response.DepositoResponse;
import com.financial.transfer.service.DepositoService;
import com.financial.transfer.service.IdempotenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/contas")
@Tag(name = "Depósitos", description = "Endpoint de depósito em conta")
public class DepositoController {

    private static final String ENDPOINT = "POST /contas/{numero}/depositos";

    private final DepositoService      depositoService;
    private final IdempotenciaService  idempotenciaService;
    private final ObjectMapper         objectMapper;

    public DepositoController(DepositoService depositoService,
                              IdempotenciaService idempotenciaService,
                              ObjectMapper objectMapper) {
        this.depositoService     = depositoService;
        this.idempotenciaService = idempotenciaService;
        this.objectMapper        = objectMapper;
    }

    @PostMapping("/{numero}/depositos")
    @Operation(summary = "Realizar depósito em conta",
               description = "Credita valor na conta. BLOQUEADA aceita. ENCERRADA rejeita.")
    public ResponseEntity<DepositoResponse> depositar(
            @PathVariable String numero,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositoRequest request,
            HttpServletRequest httpRequest) {

        String bodyJson = IdempotencyContext.extrairBody(httpRequest);

        // Verificar idempotência
        Optional<IdempotenciaService.IdempotenciaResult> resultado =
            idempotenciaService.verificar(ENDPOINT, idempotencyKey, bodyJson);

        if (resultado.isPresent()) {
            DepositoResponse resposta = idempotenciaService.deserializar(
                resultado.get().json(), DepositoResponse.class);
            return ResponseEntity.status(resultado.get().statusHttp()).body(resposta);
        }

        
        DepositoResponse resposta = depositoService.depositar(numero, request.valor());

        idempotenciaService.registrar(ENDPOINT, idempotencyKey, bodyJson, resposta,
            HttpStatus.CREATED.value());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
