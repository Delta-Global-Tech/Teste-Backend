package com.financial.transfer.controller;

import com.financial.transfer.concurrency.IdempotencyContext;
import com.financial.transfer.dto.request.AgendamentoRequest;
import com.financial.transfer.dto.response.AgendamentoResponse;
import com.financial.transfer.service.AgendamentoService;
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
@RequestMapping("/transferencias-agendadas")
@Tag(name = "Agendamentos", description = "Agendamento de transferências futuras")
public class AgendamentoController {

    private static final String ENDPOINT = "POST /transferencias-agendadas";

    private final AgendamentoService  agendamentoService;
    private final IdempotenciaService idempotenciaService;

    public AgendamentoController(AgendamentoService agendamentoService,
                                 IdempotenciaService idempotenciaService) {
        this.agendamentoService  = agendamentoService;
        this.idempotenciaService = idempotenciaService;
    }

    @PostMapping
    @Operation(summary = "Agendar transferência",
               description = "Registra agendamento. Nenhum dinheiro sai agora. Saldo verificado na execução.")
    public ResponseEntity<AgendamentoResponse> agendar(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AgendamentoRequest request,
            HttpServletRequest httpRequest) {

        String bodyJson = IdempotencyContext.extrairBody(httpRequest);

        Optional<IdempotenciaService.IdempotenciaResult> resultado =
            idempotenciaService.verificar(ENDPOINT, idempotencyKey, bodyJson);

        if (resultado.isPresent()) {
            AgendamentoResponse resposta = idempotenciaService.deserializar(
                resultado.get().json(), AgendamentoResponse.class);
            return ResponseEntity.status(resultado.get().statusHttp()).body(resposta);
        }

        AgendamentoResponse resposta = agendamentoService.agendar(
            request.contaOrigem(),
            request.contaDestino(),
            request.valor(),
            request.executarEm()
        );

        idempotenciaService.registrar(ENDPOINT, idempotencyKey, bodyJson, resposta,
            HttpStatus.CREATED.value());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
