package com.financial.transfer.controller;

import com.financial.transfer.dto.response.ExtratoResponse;
import com.financial.transfer.service.ExtratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/contas")
@Tag(name = "Extrato", description = "Extrato de movimentações da conta")
public class ExtratoController {

    private final ExtratoService extratoService;

    public ExtratoController(ExtratoService extratoService) {
        this.extratoService = extratoService;
    }

    @GetMapping("/{numero}/extrato")
    @Operation(summary = "Consultar extrato da conta",
               description = "Retorna movimentos paginados, ordenados por sequência DESC. CPF mascarado.")
    public ResponseEntity<ExtratoResponse> extrato(
            @PathVariable String numero,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(extratoService.obter(numero, de, ate, page, size));
    }
}
