package teste.delta.controllers;

import teste.delta.dto.requests.RequestTransferencia;
import teste.delta.dto.requests.RequestEstorno;
import teste.delta.dto.response.RespostaIdempotente;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import teste.delta.service.ContaService;
import teste.delta.service.TransferenciaService;

@RestController 
@RequestMapping ("/transferencias")
public class TransferenciaController {
    
    @Autowired 
    TransferenciaService transferenciaService;
    
    @Autowired 
    ContaService contaService;

    private Logger logger = LoggerFactory.getLogger(ContaService.class);

    @PostMapping()
    public ResponseEntity<String> fazerTransferencia(
        @RequestHeader (value = "Idempotency-Key") UUID idempotencyKey,
        @RequestBody RequestTransferencia requestTransferencia
    ){
        logger.info("Endpoint POST /transferencia acionado");
        RespostaIdempotente resposta = transferenciaService.fazerTransferenciaEntreContas(
            idempotencyKey, requestTransferencia
        );
        return ResponseEntity.status(resposta.status())
            .contentType(MediaType.APPLICATION_JSON)
            .body(resposta.json());
    }

    @PostMapping("/{id}/estorno")
    public ResponseEntity<String> solicitarEstorno(
        @RequestHeader (value = "Idempotency-Key") UUID idempotencyKey,
        @PathVariable ("id") UUID transferId,
        @RequestBody RequestEstorno requestEstorno
    ){
        logger.info("Endpoint POST /{id}/estorno acionado");
        RespostaIdempotente resposta = transferenciaService.fazerEstorno(idempotencyKey, transferId, requestEstorno);
        return ResponseEntity.status(resposta.status())
            .contentType(MediaType.APPLICATION_JSON)
            .body(resposta.json());
    }
}
