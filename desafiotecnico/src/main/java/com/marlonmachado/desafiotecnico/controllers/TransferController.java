package com.marlonmachado.desafiotecnico.controllers;

import com.marlonmachado.desafiotecnico.dto.DepositRequestDTO;
import com.marlonmachado.desafiotecnico.dto.TransferReceiptDTO;
import com.marlonmachado.desafiotecnico.dto.TransferRequestDTO;
import com.marlonmachado.desafiotecnico.services.TransferService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TransferController {

    @Autowired
    private  TransferService transferService;

    @PostMapping("/contas/{numero}/depositos")
    public ResponseEntity<TransferReceiptDTO> deposit(
            @PathVariable String numero,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid DepositRequestDTO request
    ) {
        TransferReceiptDTO receipt = transferService.deposit(numero, request.valor(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @PostMapping("/transferencias")
    public ResponseEntity<TransferReceiptDTO> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid TransferRequestDTO request
    ) {
        TransferReceiptDTO receipt = transferService.transfer(
                request.contaOrigem(), request.contaDestino(), request.valor(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @PostMapping("/transferencias/{id}/estorno")
    public ResponseEntity<TransferReceiptDTO> reverse(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        TransferReceiptDTO receipt = transferService.reverse(id, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

}
