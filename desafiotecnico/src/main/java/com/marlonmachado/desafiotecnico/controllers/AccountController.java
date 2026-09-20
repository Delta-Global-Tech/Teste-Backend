package com.marlonmachado.desafiotecnico.controllers;

import com.marlonmachado.desafiotecnico.dto.StatementDTO;
import com.marlonmachado.desafiotecnico.dto.SummaryDTO;
import com.marlonmachado.desafiotecnico.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class AccountController {

    @Autowired
    private AccountService accountService;


    @GetMapping("/contas/{numero}/extrato")
    public ResponseEntity<StatementDTO> getStatement(
            @PathVariable String numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        StatementDTO statement = accountService.getStatement(numero, de, ate, page, size);
        return ResponseEntity.ok(statement);
    }

    @GetMapping("/resumo")
    public ResponseEntity<SummaryDTO> getSummary(
            @RequestParam(defaultValue = "60") int janelaMinutos
    ) {
        SummaryDTO summary = accountService.getSummary(janelaMinutos);
        return ResponseEntity.ok(summary);
    }
}
