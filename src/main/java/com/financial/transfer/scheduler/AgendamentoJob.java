package com.financial.transfer.scheduler;

import com.financial.transfer.entity.Agendamento;
import com.financial.transfer.enums.EstadoAgendamento;
import com.financial.transfer.repository.AgendamentoRepository;
import com.financial.transfer.service.TransferenciaService;
import com.financial.transfer.util.MoneyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Job de processamento de agendamentos vencidos.
 */
@Component
public class AgendamentoJob {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoJob.class);

    /** Minutos para considerar um agendamento "travado" e reprocessável. */
    private static final int TIMEOUT_MINUTOS = 5;
    private static final int LOTE_TAMANHO   = 50;

    private final AgendamentoRepository agendamentoRepository;
    private final TransferenciaService  transferenciaService;
    private final TransactionTemplate   transactionTemplate;

    public AgendamentoJob(AgendamentoRepository agendamentoRepository,
                          TransferenciaService transferenciaService,
                          TransactionTemplate transactionTemplate) {
        this.agendamentoRepository = agendamentoRepository;
        this.transferenciaService  = transferenciaService;
        this.transactionTemplate   = transactionTemplate;
    }

    /**
     * Executa a cada 30 segundos.
     */
    @Scheduled(fixedDelay = 30_000)
    public void processarAgendamentos() {
        OffsetDateTime agora    = OffsetDateTime.now();
        OffsetDateTime timeout  = agora.minusMinutes(TIMEOUT_MINUTOS);

        List<Agendamento> pendentes = buscarEMarcarPendentes(agora, timeout);

        if (pendentes.isEmpty()) return;

        log.info("Job de agendamentos: processando {} agendamentos", pendentes.size());

        for (Agendamento agendamento : pendentes) {
            processarUm(agendamento);
        }
    }

    
    @Transactional
    public List<Agendamento> buscarEMarcarPendentes(OffsetDateTime agora, OffsetDateTime timeout) {
        List<Agendamento> pendentes = agendamentoRepository
            .buscarPendentesParaProcessar(agora, timeout, LOTE_TAMANHO);

        if (!pendentes.isEmpty()) {
            List<Long> ids = pendentes.stream().map(Agendamento::getId).toList();
            agendamentoRepository.marcarProcessando(ids, agora);
        }

        return pendentes;
    }

 
    private void processarUm(Agendamento agendamento) {
        transactionTemplate.execute(status -> {
            try {
                Agendamento ag = agendamentoRepository.findById(agendamento.getId())
                    .orElse(null);

                if (ag == null || ag.getEstado() != EstadoAgendamento.PENDENTE) {
                    log.debug("Agendamento {} já processado ou não encontrado", agendamento.getId());
                    return null;
                }

                log.info("Executando agendamento {} — {} → {} R$ {}",
                    ag.getId(),
                    ag.getContaOrigem().getNumero(),
                    ag.getContaDestino().getNumero(),
                    MoneyUtil.centavosParaReais(ag.getValorCentavos()));

                var transferencia = transferenciaService.transferir(
                    ag.getContaOrigem().getNumero(),
                    ag.getContaDestino().getNumero(),
                    MoneyUtil.centavosParaReais(ag.getValorCentavos())
                );

                ag.setEstado(EstadoAgendamento.EXECUTADO);
                ag.setTentativas(ag.getTentativas() + 1);
                
                // Buscar a entidade Transferencia pelo ID retornado
            
                agendamentoRepository.save(ag);

                log.info("Agendamento {} executado com sucesso. TransferenciaId={}",
                    ag.getId(), transferencia.transferenciaId());

            } catch (Exception e) {
                log.error("Falha ao processar agendamento {}: {}", agendamento.getId(), e.getMessage());
                status.setRollbackOnly();

                // Atualiza em transação separada para registrar a falha
                marcarFalhado(agendamento.getId(), e.getMessage());
            }
            return null;
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFalhado(Long agendamentoId, String motivo) {
        agendamentoRepository.findById(agendamentoId).ifPresent(ag -> {
            ag.setEstado(EstadoAgendamento.FALHADO);
            ag.setTentativas(ag.getTentativas() + 1);
            ag.setProcessandoEm(null);
            agendamentoRepository.save(ag);
        });
    }
}
