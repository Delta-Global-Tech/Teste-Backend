package teste.delta.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import teste.delta.dto.requests.RequestAgendamentoTransferencia;
import teste.delta.dto.response.RespostaIdempotente;
import teste.delta.dto.response.ResponseAgendamento;
import teste.delta.exception.ApiException;
import teste.delta.model.Agendamento;
import teste.delta.model.Conta;
import teste.delta.model.enums.EstadoAgendamento;
import teste.delta.repository.AgendamentoRepository;

@Service
public class AgendamentoService {

    private static final String ENDPOINT = "POST /transferencias-agendadas";

    private static Logger logger = LoggerFactory.getLogger(AgendamentoService.class);

    @Autowired
    AgendamentoRepository agendamentoRepository;

    @Autowired
    ContaService contaService;

    @Autowired
    IdempotenciaService idempotenciaService;

    @Transactional
    public RespostaIdempotente agendarTransferencia(UUID idempotencyKey, RequestAgendamentoTransferencia request){
        return idempotenciaService.executar(
            idempotencyKey.toString(),
            ENDPOINT,
            this.hashRequisicao(request),
            HttpStatus.CREATED,
            () -> this.criarAgendamento(request)
        );
    }

    private ResponseAgendamento criarAgendamento(RequestAgendamentoTransferencia request){
        BigDecimal valorCentavos = request.valor();
        if (!request.executarEm().isAfter(OffsetDateTime.now())) {
            throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "DATA_EXECUCAO_NO_PASSADO",
                "executarEm precisa ser uma data futura"
            );
        }

        Conta contaOrigem = this.buscarConta(request.contaOrigem());
        Conta contaDestino = this.buscarConta(request.contaDestino());

        Agendamento agendamento = new Agendamento();
        agendamento.setContaOrigemId(contaOrigem.getId());
        agendamento.setContaDestinoId(contaDestino.getId());
        agendamento.setValorCentavos(valorCentavos);
        agendamento.setExecutarEm(request.executarEm());
        agendamento.setEstado(EstadoAgendamento.AGENDADO);
        agendamento.setTentativas(0);
        Agendamento agendamentoCriado = agendamentoRepository.save(agendamento);
        logger.info("Agendamento {} criado para {}", agendamentoCriado.getId(), agendamentoCriado.getExecutarEm());

        return ResponseAgendamento.fromEntity(agendamentoCriado, contaOrigem, contaDestino);
    }

    private Conta buscarConta(String numero){
        Conta conta = contaService.getContaByNumero(numero);
        if (conta == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTA_NAO_ENCONTRADA", "Conta " + numero + " inexistente");
        }
        return conta;
    }

    // Normaliza o corpo para que 150, 150.0 e 150.00 (ou o mesmo instante em fusos diferentes) gerem o mesmo hash.
    private String hashRequisicao(RequestAgendamentoTransferencia request){
        String conteudo = String.join("|",
            request.contaOrigem(),
            request.contaDestino(),
            request.valor().stripTrailingZeros().toPlainString(),
            request.executarEm().toInstant().toString()
        );
        return idempotenciaService.hash(conteudo);
    }
}
