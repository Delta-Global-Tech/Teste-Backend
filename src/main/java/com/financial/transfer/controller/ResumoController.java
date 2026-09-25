package com.financial.transfer.controller;

import com.financial.transfer.dto.response.ResumoResponse;
import com.financial.transfer.service.ResumoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resumo")
@Tag(name = "Resumo", description = "Resumo operacional do sistema")
public class ResumoController {

    private final ResumoService resumoService;

    public ResumoController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @GetMapping
    @Operation(summary = "Resumo do sistema",
               description = "Agrega contas ativas, saldo, agendamentos, transferências e taxas numa janela de tempo.")
    public ResponseEntity<ResumoResponse> resumo(
            @RequestParam(defaultValue = "60") int janelaMinutos) {
        return ResponseEntity.ok(resumoService.obter(janelaMinutos));
    }
}
