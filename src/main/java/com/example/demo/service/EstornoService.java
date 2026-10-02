package com.example.demo.service;

import com.example.demo.dto.transferencia.TransferenciaResponse;
import com.example.demo.enums.TipoMovimento;
import com.example.demo.model.Conta;
import com.example.demo.model.Transferencia;
import com.example.demo.repository.ContaRepository;
import com.example.demo.repository.TransferenciaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoTransferencia;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class EstornoService {

    @Autowired
    private TransferenciaRepository transferenciaRepository;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private MovimentoService movimentoService;

    @Autowired
    private IdempotenciaService idempotenciaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public TransferenciaResponse estornarTransferencia(
            Long transferenciaId,
            String endpoint,
            String chave,
            String hash
    ) {

        boolean novaIdempotencia =
                idempotenciaService.registrarSeNaoExistir(
                        endpoint,
                        chave,
                        hash
                );

        if (!novaIdempotencia) {
            return recuperarRespostaIdempotente(
                    endpoint,
                    chave,
                    hash
            );
        }

        Transferencia original = transferenciaRepository
                .findByIdForUpdate(transferenciaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transferência não encontrada"
                        )
                );

        validarTransferencia(original);

        Conta origemOriginal = original.getContaOrigem();
        Conta destinoOriginal = original.getContaDestino();

        ContasEstorno contas = bloquearContas(
                destinoOriginal.getId(),
                origemOriginal.getId()
        );

        Conta novaOrigem = contas.origem();
        Conta novoDestino = contas.destino();

        long valor = original.getValorCentavos();

        if (novaOrigem.getSaldoCentavos() < valor) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar o estorno");
        }

        Transferencia estorno = criarTransferenciaEstorno(
                original,
                novaOrigem,
                novoDestino,
                valor
        );

        movimentarEstorno(
                estorno,
                novaOrigem,
                novoDestino,
                valor
        );

        original.setEstado(EstadoTransferencia.ESTORNADA);

        TransferenciaResponse response = new TransferenciaResponse(estorno);

        salvarRespostaIdempotente(
                endpoint,
                chave,
                response
        );

        return response;
    }

    private void validarTransferencia(Transferencia original) {

        if (original.getEstado() != EstadoTransferencia.CONFIRMADA) {

            throw new IllegalStateException(
                    "Somente transferências confirmadas podem ser estornadas"
            );
        }
    }

    private ContasEstorno bloquearContas(
            Long contaOrigemId,
            Long contaDestinoId
    ) {

        List<Long> ids = new ArrayList<>(
                List.of(
                        contaOrigemId,
                        contaDestinoId
                )
        );

        ids.sort(Long::compareTo);

        Conta primeira = contaRepository.findByIdForUpdate(ids.get(0)).orElseThrow();

        Conta segunda = contaRepository.findByIdForUpdate(ids.get(1)).orElseThrow();

        Conta origem = encontrarConta(
                primeira,
                segunda,
                contaOrigemId
        );

        Conta destino = encontrarConta(
                primeira,
                segunda,
                contaDestinoId
        );

        return new ContasEstorno(origem, destino);
    }

    private Conta encontrarConta(
            Conta primeira,
            Conta segunda,
            Long id
    ) {

        if (primeira.getId().equals(id)) {
            return primeira;
        }

        return segunda;
    }

    private Transferencia criarTransferenciaEstorno(
            Transferencia original,
            Conta origem,
            Conta destino,
            long valor
    ) {

        Transferencia estorno = new Transferencia(
                origem,
                destino,
                valor,
                0L,
                com.example.demo.enums.EstadoTransferencia.CONFIRMADA
        );

        estorno.setTransferenciaOriginal(original);
        estorno.setConcluidaEm(Instant.now());

        return transferenciaRepository.save(estorno);
    }

    private void movimentarEstorno(
            Transferencia estorno,
            Conta origem,
            Conta destino,
            long valor
    ) {

        origem.setSaldoCentavos(
                origem.getSaldoCentavos() - valor
        );

        movimentoService.criarMovimento(
                origem,
                estorno,
                TipoMovimento.SAIDA,
                valor
        );

        destino.setSaldoCentavos(
                destino.getSaldoCentavos() + valor
        );

        movimentoService.criarMovimento(
                destino,
                estorno,
                TipoMovimento.ENTRADA,
                valor
        );
    }

    private TransferenciaResponse recuperarRespostaIdempotente(
            String endpoint,
            String chave,
            String hash
    ) {

        var existente = idempotenciaService.buscarOuNull(
                endpoint,
                chave
        );

        if (!existente.getHashRequisicao().equals(hash)) {
            throw new IllegalArgumentException(
                    "Idempotency-Key já utilizada com outra requisição"
            );
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

    private void salvarRespostaIdempotente(
            String endpoint,
            String chave,
            TransferenciaResponse response
    ) {

        try {

            String respostaJson =
                    objectMapper.writeValueAsString(response);

            idempotenciaService.salvarResposta(
                    endpoint,
                    chave,
                    respostaJson,
                    200
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Não foi possível salvar a resposta da idempotência",
                    e
            );
        }
    }

    private record ContasEstorno(
            Conta origem,
            Conta destino
    ) {}
}