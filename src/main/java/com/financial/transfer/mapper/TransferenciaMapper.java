package com.financial.transfer.mapper;

import com.financial.transfer.dto.response.AgendamentoResponse;
import com.financial.transfer.dto.response.DepositoResponse;
import com.financial.transfer.dto.response.EstornoResponse;
import com.financial.transfer.dto.response.TransferenciaResponse;
import com.financial.transfer.entity.Agendamento;
import com.financial.transfer.entity.Movimento;
import com.financial.transfer.entity.Transferencia;
import com.financial.transfer.util.MoneyUtil;
import org.springframework.stereotype.Component;

@Component
public class TransferenciaMapper {

    public TransferenciaResponse toTransferenciaResponse(
            Transferencia t, long saldoOrigemAposCentavos) {
        long totalDebitado = t.getValorCentavos() + t.getTaxaCentavos();
        return new TransferenciaResponse(
            t.getId(),
            t.getContaOrigem().getNumero(),
            t.getContaDestino().getNumero(),
            MoneyUtil.centavosParaReais(t.getValorCentavos()),
            MoneyUtil.centavosParaReais(t.getTaxaCentavos()),
            MoneyUtil.centavosParaReais(totalDebitado),
            MoneyUtil.centavosParaReais(saldoOrigemAposCentavos),
            t.getConcluidaEm()
        );
    }

    public EstornoResponse toEstornoResponse(Transferencia estorno) {
        return new EstornoResponse(
            estorno.getId(),
            estorno.getTransferenciaOriginal().getId(),
            estorno.getContaOrigem().getNumero(),
            estorno.getContaDestino().getNumero(),
            MoneyUtil.centavosParaReais(estorno.getValorCentavos()),
            MoneyUtil.centavosParaReais(estorno.getTaxaCentavos()),
            estorno.getConcluidaEm()
        );
    }

    public DepositoResponse toDepositoResponse(Movimento movDestino, String numeroConta) {
        return new DepositoResponse(
            movDestino.getId(),
            numeroConta,
            MoneyUtil.centavosParaReais(movDestino.getValorCentavos()),
            MoneyUtil.centavosParaReais(movDestino.getSaldoAposCentavos()),
            movDestino.getCriadoEm()
        );
    }

    public AgendamentoResponse toAgendamentoResponse(Agendamento a) {
        return new AgendamentoResponse(
            a.getId(),
            a.getContaOrigem().getNumero(),
            a.getContaDestino().getNumero(),
            MoneyUtil.centavosParaReais(a.getValorCentavos()),
            a.getExecutarEm(),
            a.getEstado(),
            a.getCriadoEm()
        );
    }
}
