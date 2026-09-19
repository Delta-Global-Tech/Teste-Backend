package com.marlonmachado.desafiotecnico.controllers;

import com.marlonmachado.desafiotecnico.dto.ScheduleRequestDTO;
import com.marlonmachado.desafiotecnico.dto.ScheduleResponseDTO;
import com.marlonmachado.desafiotecnico.services.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @PostMapping("/transferencias-agendadas")
    public ResponseEntity<ScheduleResponseDTO> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid ScheduleRequestDTO request
    ) {
        ScheduleResponseDTO response = scheduleService.create(
                request.contaOrigem(), request.contaDestino(), request.valor(), request.executarEm(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
