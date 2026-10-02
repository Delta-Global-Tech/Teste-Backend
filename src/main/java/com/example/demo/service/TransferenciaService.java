package com.example.demo.service;

import com.example.demo.dto.transferencia.TransferenciaResponse;
import com.example.demo.enums.EstadoTransferencia;
import com.example.demo.enums.TipoMovimento;
import com.example.demo.model.Conta;
import com.example.demo.model.Idempotencia;
import com.example.demo.model.Transferencia;
import com.example.demo.repository.ContaRepository;
import com.example.demo.repository.TransferenciaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransferenciaService {

    @Autowired
    private TransferenciaRepository transferenciaRepository;

    @Autowired
    private IdempotenciaService idempotenciaService;

    @Autowired
    private MovimentoService movimentoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private TaxaService taxaService;

    private static final String CONTA_TAXA = "SISTEMA-TAXAS";

    @Transactional
    public TransferenciaResponse realizarTransferencia(
            Long contaOrigemId,
            Long contaDestinoId,
            long valorCentavos,
            String endpoint,
            String chave,
            String hash
    ) {

        boolean novaIdempotencia = idempotenciaService.registrarSeNaoExistir(
                        endpoint,
                        chave,
                        hash
                );

        if (!novaIdempotencia) {

            Idempotencia existente = idempotenciaService.buscarOuNull(
                    endpoint,
                    chave
            );

            if (!existente.getHashRequisicao().equals(hash)) {

                throw new IllegalArgumentException("Idempotency-Key já utilizada com outra requisição");
            }

            try {
                return objectMapper.readValue(
                        existente.getRespostaJson(),
                        TransferenciaResponse.class
                );
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Não foi possível recuperar a resposta da idempotência",
                        e
                );
            }
        }

        if (valorCentavos <= 0) {

            throw new IllegalArgumentException( "Valor de transferencia precisa ser maior que zero");
        }

        if (contaOrigemId.equals(contaDestinoId)) {
            throw new IllegalArgumentException("Conta de origem e destino precisam ser diferentes");
        }

        Conta contaTaxas = contaRepository.findByNumero(CONTA_TAXA).orElseThrow();

        ContasTransferencia contas = bloquearContas(
                contaOrigemId,
                contaDestinoId,
                contaTaxas.getId()
        );

        Conta origem = contas.origem();
        Conta destino = contas.destino();
        Conta taxas = contas.taxas();

        long usadoHoje = transferenciaRepository.somarTransferenciasDoDia(
                origem.getId(),
                EstadoTransferencia.CONFIRMADA,
                inicioDoDia(),
                inicioDoProximoDia()
        );

        if (usadoHoje + valorCentavos > origem.getLimiteDiarioCentavos()) {

            throw new IllegalArgumentException("Limite diário de transferência excedido");
        }

        long taxaCentavos = taxaService.calcularTaxa(valorCentavos);

        long totalDebitado = valorCentavos + taxaCentavos;

        if (origem.getSaldoCentavos() < totalDebitado) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }

        Transferencia transferencia = new Transferencia(
                origem,
                destino,
                valorCentavos,
                taxaCentavos,
                EstadoTransferencia.CONFIRMADA
        );

        transferencia.setConcluidaEm(Instant.now());

        transferencia = transferenciaRepository.save(transferencia);

        origem.setSaldoCentavos(origem.getSaldoCentavos() - valorCentavos);
        movimentoService.criarMovimento(
                origem,
                transferencia,
                TipoMovimento.SAIDA,
                valorCentavos
        );

        destino.setSaldoCentavos(destino.getSaldoCentavos() + valorCentavos);
        movimentoService.criarMovimento(
                destino,
                transferencia,
                TipoMovimento.ENTRADA,
                valorCentavos
        );

        if (taxaCentavos > 0) {

            // debita a taxa separadamente
            origem.setSaldoCentavos(origem.getSaldoCentavos() - taxaCentavos);

            movimentoService.criarMovimento(
                    origem,
                    transferencia,
                    TipoMovimento.SAIDA,
                    taxaCentavos
            );

            // credita a taxa em SISTEMA-TAXAS
            taxas.setSaldoCentavos(taxas.getSaldoCentavos() + taxaCentavos);

            movimentoService.criarMovimento(
                    taxas,
                    transferencia,
                    TipoMovimento.ENTRADA,
                    taxaCentavos
            );
        }

        TransferenciaResponse response = new TransferenciaResponse(transferencia);
        // transforma resposta em json e guarda
        try {

            String respostaJson = objectMapper.writeValueAsString(response);

            idempotenciaService.salvarResposta(
                    endpoint,
                    chave,
                    respostaJson,
                    201
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Não foi possível salvar a resposta da idempotência",
                    e
            );
        }

        return response;
    }

    private ContasTransferencia bloquearContas(
            Long contaOrigemId,
            Long contaDestinoId,
            Long contaTaxaId
    ) {

        List<Long> ids = new ArrayList<>(
                List.of(
                        contaOrigemId,
                        contaDestinoId,
                        contaTaxaId
                )
        );

        // bloqueia do menor para o maior. escolhi unicamente para manter um padrão
        ids.sort(Long::compareTo);

        Conta primeira = contaRepository.findByIdForUpdate(ids.get(0)).orElseThrow();
        Conta segunda = contaRepository.findByIdForUpdate(ids.get(1)).orElseThrow();
        Conta terceira = contaRepository.findByIdForUpdate(ids.get(2)).orElseThrow();

        Conta origem = encontrarConta(
                primeira,
                segunda,
                terceira,
                contaOrigemId
        );

        Conta destino = encontrarConta(
                primeira,
                segunda,
                terceira,
                contaDestinoId
        );

        Conta taxas = encontrarConta(
                primeira,
                segunda,
                terceira,
                contaTaxaId
        );

        return new ContasTransferencia(
                origem,
                destino,
                taxas
        );
    }

    private Conta encontrarConta(
            Conta primeira,
            Conta segunda,
            Conta terceira,
            Long id
    ) {

        if (primeira.getId().equals(id)) {
            return primeira;
        }

        if (segunda.getId().equals(id)) {
            return segunda;
        }

        return terceira;
    }

    // objeto que guardars as contas
    private record ContasTransferencia(
            Conta origem,
            Conta destino,
            Conta taxas
    ) {}

    private Instant inicioDoDia() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                .atStartOfDay(ZoneId.of("America/Sao_Paulo"))
                .toInstant();
    }

    private Instant inicioDoProximoDia() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                .plusDays(1)
                .atStartOfDay(ZoneId.of("America/Sao_Paulo"))
                .toInstant();
    }

}