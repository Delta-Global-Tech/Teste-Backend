package com.example.demo.service;

import com.example.demo.enums.TipoMovimento;
import com.example.demo.model.Conta;
import com.example.demo.model.Movimento;
import com.example.demo.model.Transferencia;
import com.example.demo.repository.MovimentoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MovimentoService {

    @Autowired
    private MovimentoRepository movimentoRepository;

    @Transactional
    public Movimento criarMovimento(
            Conta conta,
            Transferencia transferencia,
            TipoMovimento tipo,
            long valorCentavos
    ) {
        long proximaSequencia = movimentoRepository.findMaiorSequencia(conta.getId()) + 1;

        Movimento movimento = new Movimento();
        movimento.setSequencia(proximaSequencia);
        movimento.setConta(conta);
        movimento.setTransferencia(transferencia);
        movimento.setTipo(tipo);
        movimento.setValorCentavos(valorCentavos);
        movimento.setSaldoAposCentavos(conta.getSaldoCentavos());
        movimento.setCriadoEm(Instant.now());

        return movimentoRepository.save(movimento);
    }
}