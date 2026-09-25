package com.financial.transfer.service;

import com.financial.transfer.dto.response.EstornoResponse;
import com.financial.transfer.entity.Conta;
import com.financial.transfer.entity.Transferencia;
import com.financial.transfer.enums.EstadoTransferencia;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.mapper.TransferenciaMapper;
import com.financial.transfer.repository.ContaRepository;
import com.financial.transfer.repository.TransferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class EstornoService {

    private static final String CONTA_SISTEMA_TAXAS = "SISTEMA-TAXAS";

    private final TransferenciaRepository  transferenciaRepository;
    private final ContaRepository          contaRepository;
    private final EstadoTransferenciaService estadoTransferenciaService;
    private final MovimentoService         movimentoService;
    private final TransferenciaMapper      mapper;

    public EstornoService(TransferenciaRepository transferenciaRepository,
                          ContaRepository contaRepository,
                          EstadoTransferenciaService estadoTransferenciaService,
                          MovimentoService movimentoService,
                          TransferenciaMapper mapper) {
        this.transferenciaRepository    = transferenciaRepository;
        this.contaRepository            = contaRepository;
        this.estadoTransferenciaService = estadoTransferenciaService;
        this.movimentoService           = movimentoService;
        this.mapper                     = mapper;
    }

    @Transactional
    public EstornoResponse estornar(Long transferenciaId, String motivo) {

        Transferencia original = transferenciaRepository.findByIdForUpdate(transferenciaId)
        .orElseThrow(() -> ApiException.transferenciaNaoEncontrada(transferenciaId));

        if (original.getEstado() == EstadoTransferencia.ESTORNADA) {
    throw ApiException.transferenciaJaEstornada(transferenciaId);
}

        if (original.getEstado() != EstadoTransferencia.CONFIRMADA) {
            throw ApiException.transferenciaNaoConfirmada(transferenciaId);
}

        // Verifica duplo estorno antes de adquirir locks
        boolean jaEstornada = transferenciaRepository
            .existsByTransferenciaOriginalIdAndEstado(
              transferenciaId,
            EstadoTransferencia.ESTORNADA
    );

        if (jaEstornada) {
            throw ApiException.transferenciaJaEstornada(transferenciaId);
}

        Long idOrigem  = original.getContaOrigem().getId();
        Long idDestino = original.getContaDestino().getId();

        List<Long> idsOrdenados = List.of(idOrigem, idDestino).stream().sorted().toList();
        List<Conta> contasLockadas = contaRepository.findAllByIdWithLock(idsOrdenados);

        Conta contaQueEnvia    = contasLockadas.stream()
            .filter(c -> c.getId().equals(idDestino)).findFirst().orElseThrow();
        Conta contaQueRecebe   = contasLockadas.stream()
            .filter(c -> c.getId().equals(idOrigem)).findFirst().orElseThrow();

        long valorCentavos = original.getValorCentavos();
        long taxaCentavos  = original.getTaxaCentavos();

        long totalEstorno = valorCentavos; 
        if (!contaQueEnvia.isContaSistema() && contaQueEnvia.getSaldoCentavos() < totalEstorno) {
            throw ApiException.estornoSemSaldo();
        }

        Transferencia originalFresh = transferenciaRepository.findById(transferenciaId).orElseThrow();
        if (originalFresh.getEstado() != EstadoTransferencia.CONFIRMADA) {
            throw ApiException.transferenciaJaEstornada(transferenciaId);
        }

        estadoTransferenciaService.transicionar(originalFresh, EstadoTransferencia.ESTORNADA);
        transferenciaRepository.save(originalFresh);

        Transferencia estorno = new Transferencia();
        estorno.setContaOrigem(contaQueEnvia);
        estorno.setContaDestino(contaQueRecebe);
        estorno.setValorCentavos(valorCentavos);
        estorno.setTaxaCentavos(0L); // estorno não gera nova taxa
        estorno.setEstado(EstadoTransferencia.CONFIRMADA);
        estorno.setTransferenciaOriginal(originalFresh);
        estorno.setConcluidaEm(OffsetDateTime.now());
        estorno = transferenciaRepository.save(estorno);

        movimentoService.registrarSaida(contaQueEnvia, estorno, valorCentavos);
        movimentoService.registrarEntrada(contaQueRecebe, estorno, valorCentavos);

        if (taxaCentavos > 0) {
            Conta sistemaTaxas = contaRepository.findByNumeroWithLock(CONTA_SISTEMA_TAXAS)
                .orElseThrow(() -> new IllegalStateException("SISTEMA-TAXAS não encontrada"));
            movimentoService.registrarSaida(sistemaTaxas, estorno, taxaCentavos);
            movimentoService.registrarEntrada(contaQueRecebe, estorno, taxaCentavos);
            contaRepository.save(sistemaTaxas);
        }

        contaRepository.save(contaQueEnvia);
        contaRepository.save(contaQueRecebe);

        return mapper.toEstornoResponse(estorno);
    }
}
