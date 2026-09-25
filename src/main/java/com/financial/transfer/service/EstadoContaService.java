package com.financial.transfer.service;

import com.financial.transfer.entity.Conta;
import com.financial.transfer.enums.EstadoConta;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EstadoContaService {
    public void transicionar(Conta conta, EstadoConta novoEstado) {
        EstadoConta estadoAtual = conta.getEstado();
        if (!estadoAtual.podeTransicionarPara(novoEstado)) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.TRANSICAO_INVALIDA,
                "Transição inválida: " + estadoAtual + " → " + novoEstado
                    + " para a conta " + conta.getNumero()
            );
        }
        conta.setEstado(novoEstado);
    }

    public void validarPodeEnviar(Conta conta) {
        EstadoConta estado = conta.getEstado();
        if (estado == EstadoConta.ENCERRADA) {
            throw ApiException.contaEncerrada(conta.getNumero());
        }
        if (estado == EstadoConta.BLOQUEADA) {
            throw ApiException.contaBloqueada(conta.getNumero());
        }
        // ATIVA: ok
    }

    public void validarPodeReceber(Conta conta) {
        if (conta.getEstado() == EstadoConta.ENCERRADA) {
            throw ApiException.contaDestinoEncerrada(conta.getNumero());
        }
        // ATIVA e BLOQUEADA: ok para receber
    }
}
