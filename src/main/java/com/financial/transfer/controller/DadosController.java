package com.financial.transfer.controller;

import com.financial.transfer.service.DadosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dados")
@Tag(name = "Dados (Dev)", description = "Reset de dados/testes")
public class DadosController {

    private final DadosService dadosService;

    public DadosController(DadosService dadosService) {
        this.dadosService = dadosService;
    }

    @DeleteMapping
    @Operation(summary = "Resetar dados",
               description = "Apaga movimentos, transferências, agendamentos e idempotências. Restaura saldos iniciais.")
    public ResponseEntity<Map<String, String>> resetar() {
        dadosService.resetar();
        System.out.println();
        return ResponseEntity.ok(Map.of("status", "dados resetados com sucesso"));
    }
}
