package com.financial.transfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.transfer.concurrency.IdempotencyContext;
import com.financial.transfer.dto.request.EstornoRequest;
import com.financial.transfer.dto.request.TransferenciaRequest;
import com.financial.transfer.dto.response.EstornoResponse;
import com.financial.transfer.dto.response.TransferenciaResponse;
import com.financial.transfer.service.EstornoService;
import com.financial.transfer.service.IdempotenciaService;
import com.financial.transfer.service.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transferencias")
@Tag(name = "Transferências", description = "Transferências e estornos")
public class TransferenciaController {

    private static final String ENDPOINT_TRANSFERENCIA = "POST /transferencias";
    private static final String ENDPOINT_ESTORNO = "POST /transferencias/{id}/estorno";

    private final TransferenciaService transferenciaService;
    private final EstornoService estornoService;
    private final IdempotenciaService idempotenciaService;
    private final ObjectMapper objectMapper;

    public TransferenciaController(TransferenciaService transferenciaService,
            EstornoService estornoService,
            IdempotenciaService idempotenciaService,
            ObjectMapper objectMapper) {
        this.transferenciaService = transferenciaService;
        this.estornoService = estornoService;
        this.idempotenciaService = idempotenciaService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @PostMapping
    @Operation(summary = "Realizar transferência entre contas",
            description = "Debita origem, credita destino, aplica taxa. Idempotente.")
    public ResponseEntity<TransferenciaResponse> transferir(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferenciaRequest request,
            HttpServletRequest httpRequest) {

        String bodyJson = IdempotencyContext.extrairBody(httpRequest);

        idempotenciaService.adquirirLockTransacional(
                ENDPOINT_TRANSFERENCIA,
                idempotencyKey
        );

        Optional<IdempotenciaService.IdempotenciaResult> resultado
                = idempotenciaService.verificar(
                        ENDPOINT_TRANSFERENCIA,
                        idempotencyKey,
                        bodyJson
                );

        if (resultado.isPresent()) {
            TransferenciaResponse resposta = idempotenciaService.deserializar(
                    resultado.get().json(), TransferenciaResponse.class);
            return ResponseEntity.status(resultado.get().statusHttp()).body(resposta);
        }

        TransferenciaResponse resposta = transferenciaService.transferir(
                request.contaOrigem(), request.contaDestino(), request.valor());

        idempotenciaService.registrar(ENDPOINT_TRANSFERENCIA, idempotencyKey, bodyJson, resposta,
                HttpStatus.CREATED.value());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/{id}/estorno")
    @Operation(summary = "Estornar transferência",
            description = "Cria transferência inversa. Somente CONFIRMADA pode ser estornada.")
    public ResponseEntity<EstornoResponse> estornar(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EstornoRequest request,
            HttpServletRequest httpRequest) {

        String endpoint = "POST /transferencias/" + id + "/estorno";
        String bodyJson = IdempotencyContext.extrairBody(httpRequest);

        Optional<IdempotenciaService.IdempotenciaResult> resultado
                = idempotenciaService.verificar(endpoint, idempotencyKey, bodyJson);

        if (resultado.isPresent()) {
            EstornoResponse resposta = idempotenciaService.deserializar(
                    resultado.get().json(), EstornoResponse.class);
            return ResponseEntity.status(resultado.get().statusHttp()).body(resposta);
        }

        EstornoResponse resposta = estornoService.estornar(id, request.motivo());

        idempotenciaService.registrar(endpoint, idempotencyKey, bodyJson, resposta,
                HttpStatus.CREATED.value());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
