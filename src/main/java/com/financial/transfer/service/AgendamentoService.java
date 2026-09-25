package com.financial.transfer.service;

import com.financial.transfer.dto.response.AgendamentoResponse;
import com.financial.transfer.entity.Agendamento;
import com.financial.transfer.entity.Conta;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.mapper.TransferenciaMapper;
import com.financial.transfer.repository.AgendamentoRepository;
import com.financial.transfer.repository.ContaRepository;
import com.financial.transfer.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ContaRepository       contaRepository;
    private final TransferenciaMapper   mapper;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              ContaRepository contaRepository,
                              TransferenciaMapper mapper) {
        this.agendamentoRepository = agendamentoRepository;
        this.contaRepository       = contaRepository;
        this.mapper                = mapper;
    }

    @Transactional
    public AgendamentoResponse agendar(String numeroOrigem,
                                       String numeroDestino,
                                       BigDecimal valor,
                                       OffsetDateTime executarEm) {
        if (numeroOrigem.equals(numeroDestino)) {
            throw ApiException.contasIguais();
        }

        long valorCentavos = MoneyUtil.reaisParaCentavos(valor);
        if (valorCentavos <= 0) {
            throw ApiException.valorInvalido("O valor do agendamento deve ser maior que zero");
        }

        // Valida data (bean validation @Future já cobre, mas validamos aqui também)
        if (!executarEm.isAfter(OffsetDateTime.now())) {
            throw ApiException.dataPassado();
        }

        // Verifica existência das contas (sem lock — apenas validação de agendamento)
        Conta origem  = contaRepository.findByNumero(numeroOrigem)
            .orElseThrow(() -> ApiException.contaNaoEncontrada(numeroOrigem));
        Conta destino = contaRepository.findByNumero(numeroDestino)
            .orElseThrow(() -> ApiException.contaNaoEncontrada(numeroDestino));

        Agendamento agendamento = new Agendamento();
        agendamento.setContaOrigem(origem);
        agendamento.setContaDestino(destino);
        agendamento.setValorCentavos(valorCentavos);
        agendamento.setExecutarEm(executarEm);

        agendamento = agendamentoRepository.save(agendamento);

        return mapper.toAgendamentoResponse(agendamento);
    }
}
