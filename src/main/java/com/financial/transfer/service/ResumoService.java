package com.financial.transfer.service;

import com.financial.transfer.dto.response.ResumoResponse;
import com.financial.transfer.enums.EstadoConta;
import com.financial.transfer.repository.AgendamentoRepository;
import com.financial.transfer.repository.ContaRepository;
import com.financial.transfer.repository.MovimentoRepository;
import com.financial.transfer.repository.TransferenciaRepository;
import com.financial.transfer.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ResumoService {

    private final ContaRepository        contaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final AgendamentoRepository  agendamentoRepository;
    private final MovimentoRepository    movimentoRepository;

    public ResumoService(ContaRepository contaRepository,
                         TransferenciaRepository transferenciaRepository,
                         AgendamentoRepository agendamentoRepository,
                         MovimentoRepository movimentoRepository) {
        this.contaRepository        = contaRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.agendamentoRepository  = agendamentoRepository;
        this.movimentoRepository    = movimentoRepository;
    }

    @Transactional(readOnly = true)
    public ResumoResponse obter(int janelaMinutos) {
        OffsetDateTime desde = OffsetDateTime.now().minusMinutes(janelaMinutos);

        long   contasAtivas           = contaRepository.countByEstado(EstadoConta.ATIVA);
        long   saldoTotalCentavos     = transferenciaRepository.somasSaldoUsuarios();
        long   agendamentosPendentes  = agendamentoRepository.countPendentes();
        long   transferenciasJanela   = transferenciaRepository.contarTransferenciasNaJanela(desde);
        long   valorJanelaCentavos    = transferenciaRepository.somaValorNaJanela(desde);
        long   taxasJanelaCentavos    = transferenciaRepository.somaTaxasNaJanela(desde);
        Double ticketMedioCentavos    = transferenciaRepository.ticketMedioNaJanela(desde);
        long   estornosJanela         = transferenciaRepository.contarEstornosNaJanela(desde);
        long   somaMovimentos         = movimentoRepository.somaGlobal();

        return new ResumoResponse(
            contasAtivas,
            MoneyUtil.centavosParaReais(saldoTotalCentavos),
            agendamentosPendentes,
            transferenciasJanela,
            MoneyUtil.centavosParaReais(valorJanelaCentavos),
            MoneyUtil.centavosParaReais(taxasJanelaCentavos),
            MoneyUtil.centavosParaReais(ticketMedioCentavos),
            estornosJanela,
            somaMovimentos == 0L
        );
    }
}
