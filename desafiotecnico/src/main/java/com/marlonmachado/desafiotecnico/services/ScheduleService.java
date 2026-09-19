package com.marlonmachado.desafiotecnico.services;


import com.marlonmachado.desafiotecnico.dto.ScheduleRequestDTO;
import com.marlonmachado.desafiotecnico.dto.ScheduleResponseDTO;
import com.marlonmachado.desafiotecnico.entities.Account;
import com.marlonmachado.desafiotecnico.entities.IdempotencyRecord;
import com.marlonmachado.desafiotecnico.entities.Schedule;
import com.marlonmachado.desafiotecnico.entities.enums.ScheduleStatus;
import com.marlonmachado.desafiotecnico.exceptions.BusinessException;
import com.marlonmachado.desafiotecnico.repositories.AccountRepository;
import com.marlonmachado.desafiotecnico.repositories.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class ScheduleService {

    private static final String ENDPOINT_AGENDAMENTO = "POST /transferencias-agendadas";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private IdempotencyService idempotencyService;

    @Transactional
    public ScheduleResponseDTO create(String contaOrigemNumero, String contaDestinoNumero, Double valorEmReais,
                                      Instant executarEm, String idempotencyKey) {

        ScheduleRequestDTO corpo = new ScheduleRequestDTO(contaOrigemNumero, contaDestinoNumero, valorEmReais, executarEm);

        Optional<IdempotencyRecord> registroExistente = idempotencyService.buscarRegistroExistente(
                ENDPOINT_AGENDAMENTO, idempotencyKey, corpo);

        if (registroExistente.isPresent()) {
            return idempotencyService.lerResposta(registroExistente.get(), ScheduleResponseDTO.class);
        }

        long valorCentavos = Math.round(valorEmReais * 100);

        if (valorCentavos <= 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALOR_INVALIDO",
                    "Valor do agendamento deve ser maior que zero");
        }
        if (executarEm.isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "DATA_INVALIDA",
                    "Data de execução não pode estar no passado");
        }

        Account origem = accountRepository.findByNumber(contaOrigemNumero)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "Conta de origem não encontrada: " + contaOrigemNumero));
        Account destino = accountRepository.findByNumber(contaDestinoNumero)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "Conta de destino não encontrada: " + contaDestinoNumero));

        Schedule agendamento = new Schedule(null, origem, destino, valorCentavos, executarEm,
                ScheduleStatus.SCHEDULED, 0, null, null);
        agendamento = scheduleRepository.save(agendamento);

        ScheduleResponseDTO resposta = new ScheduleResponseDTO(
                agendamento.getId(),
                contaOrigemNumero,
                contaDestinoNumero,
                valorEmReais,
                executarEm.toString(),
                agendamento.getState().name()
        );

        idempotencyService.salvarRegistro(ENDPOINT_AGENDAMENTO, idempotencyKey, corpo, resposta, 201);

        return resposta;
    }
}
