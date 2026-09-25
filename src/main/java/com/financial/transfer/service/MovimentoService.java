package com.financial.transfer.service;

import com.financial.transfer.entity.Conta;
import com.financial.transfer.entity.Movimento;
import com.financial.transfer.entity.Transferencia;
import com.financial.transfer.enums.TipoMovimento;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.repository.MovimentoRepository;
import org.springframework.stereotype.Service;

@Service
public class MovimentoService {

    private final MovimentoRepository movimentoRepository;

    public MovimentoService(MovimentoRepository movimentoRepository) {
        this.movimentoRepository = movimentoRepository;
    }

   
    public Movimento registrarSaida(Conta conta, Transferencia transferencia, long valorCentavos) {
        if (valorCentavos <= 0) {
            throw new IllegalArgumentException("valorCentavos deve ser positivo: " + valorCentavos);
        }

        long novoSaldo = conta.getSaldoCentavos() - valorCentavos;

        // Contas de usuário não podem ficar negativas
        if (!conta.isContaSistema() && novoSaldo < 0) {
            throw ApiException.saldoInsuficiente();
        }

        long sequencia = movimentoRepository.proximaSequencia(conta.getId());

        Movimento mov = new Movimento();
        mov.setConta(conta);
        mov.setTransferencia(transferencia);
        mov.setSequencia(sequencia);
        mov.setTipo(TipoMovimento.SAIDA);
        mov.setValorCentavos(valorCentavos);
        mov.setSaldoAposCentavos(novoSaldo);

        conta.setSaldoCentavos(novoSaldo);

        return movimentoRepository.save(mov);
    }

  
    public Movimento registrarEntrada(Conta conta, Transferencia transferencia, long valorCentavos) {
        if (valorCentavos <= 0) {
            throw new IllegalArgumentException("valorCentavos deve ser positivo: " + valorCentavos);
        }

        long novoSaldo = conta.getSaldoCentavos() + valorCentavos;
        long sequencia = movimentoRepository.proximaSequencia(conta.getId());

        Movimento mov = new Movimento();
        mov.setConta(conta);
        mov.setTransferencia(transferencia);
        mov.setSequencia(sequencia);
        mov.setTipo(TipoMovimento.ENTRADA);
        mov.setValorCentavos(valorCentavos);
        mov.setSaldoAposCentavos(novoSaldo);

        conta.setSaldoCentavos(novoSaldo);

        return movimentoRepository.save(mov);
    }
}
