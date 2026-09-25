package com.financial.transfer.mapper;

import com.financial.transfer.dto.response.MovimentoResponse;
import com.financial.transfer.entity.Movimento;
import com.financial.transfer.util.MoneyUtil;
import org.springframework.stereotype.Component;

@Component
public class MovimentoMapper {

    public MovimentoResponse toResponse(Movimento m) {
        return new MovimentoResponse(
            m.getSequencia(),
            m.getTipo(),
            MoneyUtil.centavosParaReais(m.getValorCentavos()),
            MoneyUtil.centavosParaReais(m.getSaldoAposCentavos()),
            m.getCriadoEm(),
            m.getTransferencia() != null ? m.getTransferencia().getId() : null
        );
    }
}
