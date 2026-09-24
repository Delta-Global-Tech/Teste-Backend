package teste.delta.controllers;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import teste.delta.dto.requests.RequestDeposito;
import teste.delta.dto.response.ResponseExtrato;
import teste.delta.dto.response.RespostaIdempotente;
import teste.delta.service.ContaService;

@RestController 
@RequestMapping ("/contas")
public class ContaController {

    private Logger logger = LoggerFactory.getLogger(ContaService.class);

    @Autowired 
    ContaService contaService;

    @GetMapping()
    public String testEndpoint(){
        return "Hello";
    }

    @GetMapping("/{numero}/extrato")
    public ResponseExtrato consultarExtrato(
        @PathVariable("numero") String numeroConta,
        @RequestParam(value = "de", required = false) LocalDate de,
        @RequestParam(value = "ate", required = false) LocalDate ate,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ){
        logger.info("Endpoint GET /contas/{numero}/extrato acionado");
        return contaService.consultarExtrato(numeroConta, de, ate, page, size);
    }

    @PostMapping("/{numero}/depositos")
    public ResponseEntity<String> fazerDeposito(
        @RequestHeader (value = "Idempotency-Key") UUID idempotencyKey,
        @PathVariable("numero") String numeroConta,
        @RequestBody RequestDeposito requestDeposito){
            logger.info("Endpoint /deposito foi chamado");
            RespostaIdempotente resposta = contaService.fazerDeposito(idempotencyKey, numeroConta, requestDeposito);
            return ResponseEntity.status(resposta.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(resposta.json());
    }
}
