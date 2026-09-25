package com.financial.transfer.service;

import com.financial.transfer.entity.Transferencia;
import com.financial.transfer.enums.EstadoTransferencia;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EstadoTransferenciaService {

    public void transicionar(Transferencia transferencia, EstadoTransferencia novoEstado) {
        EstadoTransferencia estadoAtual = transferencia.getEstado();
        if (!estadoAtual.podeTransicionarPara(novoEstado)) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.TRANSICAO_INVALIDA,
                "Transição inválida: " + estadoAtual + " → " + novoEstado
                    + " para a transferência " + transferencia.getId()
            );
        }
        transferencia.setEstado(novoEstado);
    }
}
