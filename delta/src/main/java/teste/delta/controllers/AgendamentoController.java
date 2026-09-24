package teste.delta.controllers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import teste.delta.dto.requests.RequestAgendamentoTransferencia;
import teste.delta.dto.response.RespostaIdempotente;
import teste.delta.service.AgendamentoService;

@RestController
@RequestMapping ("/transferencias-agendadas")
public class AgendamentoController {

    private Logger logger = LoggerFactory.getLogger(AgendamentoController.class);

    @Autowired
    AgendamentoService agendamentoService;

    @PostMapping()
    public ResponseEntity<String> agendarTransferencia(
        @RequestHeader (value = "Idempotency-Key") UUID idempotencyKey,
        @Valid @RequestBody RequestAgendamentoTransferencia request
    ){
        logger.info("Endpoint POST /transferencias-agendadas acionado");
        RespostaIdempotente resposta = agendamentoService.agendarTransferencia(idempotencyKey, request);
        return ResponseEntity.status(resposta.status())
            .contentType(MediaType.APPLICATION_JSON)
            .body(resposta.json());
    }
}
