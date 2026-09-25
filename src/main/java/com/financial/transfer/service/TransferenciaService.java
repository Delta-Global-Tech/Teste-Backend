package com.financial.transfer.service;

import com.financial.transfer.dto.response.TransferenciaResponse;
import com.financial.transfer.entity.Conta;
import com.financial.transfer.entity.Transferencia;
import com.financial.transfer.enums.EstadoTransferencia;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.mapper.TransferenciaMapper;
import com.financial.transfer.repository.ContaRepository;
import com.financial.transfer.repository.TransferenciaRepository;
import com.financial.transfer.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TransferenciaService {

    private static final String CONTA_SISTEMA_TAXAS = "SISTEMA-TAXAS";

    private final ContaRepository contaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final TaxaService taxaService;
    private final EstadoContaService estadoContaService;
    private final LimiteDiarioService limiteDiarioService;
    private final MovimentoService movimentoService;
    private final TransferenciaMapper mapper;

    public TransferenciaService(ContaRepository contaRepository,
            TransferenciaRepository transferenciaRepository,
            TaxaService taxaService,
            EstadoContaService estadoContaService,
            LimiteDiarioService limiteDiarioService,
            MovimentoService movimentoService,
            TransferenciaMapper mapper) {
        this.contaRepository = contaRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.taxaService = taxaService;
        this.estadoContaService = estadoContaService;
        this.limiteDiarioService = limiteDiarioService;
        this.movimentoService = movimentoService;
        this.mapper = mapper;
    }

    @Transactional
    public TransferenciaResponse transferir(String numeroOrigem, String numeroDestino, BigDecimal valor) {

        if (numeroOrigem.equals(numeroDestino)) {
            throw ApiException.contasIguais();
        }

        long valorCentavos = MoneyUtil.reaisParaCentavos(valor);
        if (valorCentavos <= 0) {
            throw ApiException.valorInvalido("O valor da transferência deve ser maior que zero");
        }

        List<String> numerosOrdenados = List.of(numeroOrigem, numeroDestino)
                .stream()
                .sorted()
                .toList();

        List<Conta> contasLockadas
                = contaRepository.findAllByNumeroWithLock(numerosOrdenados);

        if (contasLockadas.size() != 2) {
            throw ApiException.contaNaoEncontrada(
                    contasLockadas.size() == 0
                    ? numeroOrigem
                    : numeroDestino
            );
        }

        Conta conta1 = contasLockadas.get(0);
        Conta conta2 = contasLockadas.get(1);

        Conta origem = conta1.getNumero().equals(numeroOrigem)
                ? conta1
                : conta2;

        Conta destino = conta1.getNumero().equals(numeroDestino)
                ? conta1
                : conta2;

        estadoContaService.validarPodeEnviar(origem);
        estadoContaService.validarPodeReceber(destino);

        long taxaCentavos = taxaService.calcular(valorCentavos);
        long totalDebitado = valorCentavos + taxaCentavos;

        if (origem.getSaldoCentavos() < totalDebitado) {
            throw ApiException.saldoInsuficiente();
        }

        limiteDiarioService.validar(origem, valorCentavos);

        Transferencia t = new Transferencia();
        t.setContaOrigem(origem);
        t.setContaDestino(destino);
        t.setValorCentavos(valorCentavos);
        t.setTaxaCentavos(taxaCentavos);
        t.setEstado(EstadoTransferencia.CONFIRMADA);
        t.setConcluidaEm(OffsetDateTime.now());
        t = transferenciaRepository.save(t);

        movimentoService.registrarSaida(origem, t, valorCentavos);
        movimentoService.registrarEntrada(destino, t, valorCentavos);

        if (taxaCentavos > 0) {
            Conta sistemaTaxas = contaRepository.findByNumeroWithLock(CONTA_SISTEMA_TAXAS)
                    .orElseThrow(() -> new IllegalStateException("SISTEMA-TAXAS não encontrada"));
            movimentoService.registrarSaida(origem, t, taxaCentavos);
            movimentoService.registrarEntrada(sistemaTaxas, t, taxaCentavos);
            contaRepository.save(sistemaTaxas);
        }

        contaRepository.save(origem);
        contaRepository.save(destino);

        return mapper.toTransferenciaResponse(t, origem.getSaldoCentavos());
    }
}
