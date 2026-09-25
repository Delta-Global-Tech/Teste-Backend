package com.financial.transfer.service;

import com.financial.transfer.dto.response.DepositoResponse;
import com.financial.transfer.entity.Conta;
import com.financial.transfer.entity.Movimento;
import com.financial.transfer.entity.Transferencia;
import com.financial.transfer.enums.EstadoTransferencia;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.repository.ContaRepository;
import com.financial.transfer.repository.TransferenciaRepository;
import com.financial.transfer.mapper.TransferenciaMapper;
import com.financial.transfer.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class DepositoService {

    private static final String CONTA_SISTEMA_ENTRADA = "SISTEMA-ENTRADA";
    private static final long   VALOR_MAX_CENTAVOS    = 1_000_000L; // R$ 10.000,00

    private final ContaRepository        contaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final EstadoContaService     estadoContaService;
    private final MovimentoService       movimentoService;
    private final TransferenciaMapper    mapper;

    public DepositoService(ContaRepository contaRepository,
                           TransferenciaRepository transferenciaRepository,
                           EstadoContaService estadoContaService,
                           MovimentoService movimentoService,
                           TransferenciaMapper mapper) {
        this.contaRepository        = contaRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.estadoContaService     = estadoContaService;
        this.movimentoService       = movimentoService;
        this.mapper                 = mapper;
    }

  
    @Transactional
    public DepositoResponse depositar(String numeroConta, BigDecimal valor) {
        long valorCentavos = MoneyUtil.reaisParaCentavos(valor);
        if (valorCentavos <= 0 || valorCentavos > VALOR_MAX_CENTAVOS) {
            throw ApiException.valorInvalido(
                "Valor deve ser entre R$ 0,01 e R$ 10.000,00");
        }

        Conta destino = contaRepository.findByNumeroWithLock(numeroConta)
            .orElseThrow(() -> ApiException.contaNaoEncontrada(numeroConta));

        estadoContaService.validarPodeReceber(destino);

        Conta sistemaEntrada = contaRepository.findByNumeroWithLock(CONTA_SISTEMA_ENTRADA)
            .orElseThrow(() -> new IllegalStateException("SISTEMA-ENTRADA não encontrada"));

        // Criar transferência
        Transferencia t = new Transferencia();
        t.setContaOrigem(sistemaEntrada);
        t.setContaDestino(destino);
        t.setValorCentavos(valorCentavos);
        t.setTaxaCentavos(0L);
        t.setEstado(EstadoTransferencia.CONFIRMADA);
        t.setConcluidaEm(OffsetDateTime.now());
        t = transferenciaRepository.save(t);

        movimentoService.registrarSaida(sistemaEntrada, t, valorCentavos);
        Movimento movDestino = movimentoService.registrarEntrada(destino, t, valorCentavos);

        contaRepository.save(sistemaEntrada);
        contaRepository.save(destino);

        return mapper.toDepositoResponse(movDestino, numeroConta);
    }
}
