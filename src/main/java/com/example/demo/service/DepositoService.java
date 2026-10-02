package com.example.demo.service;

import com.example.demo.dto.conta.ContaResponse;
import com.example.demo.enums.EstadoConta;
import com.example.demo.enums.TipoMovimento;
import com.example.demo.model.Conta;
import com.example.demo.model.Idempotencia;
import com.example.demo.repository.ContaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DepositoService {

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private MovimentoService movimentoService;

    @Autowired
    private IdempotenciaService idempotenciaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public ContaResponse realizarDeposito(
            String numeroConta,
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
            Idempotencia existente =
                    idempotenciaService.buscarOuNull(
                            endpoint,
                            chave
                    );

            if (!existente.getHashRequisicao().equals(hash)) {
                throw new IllegalArgumentException("Idempotency-Key já utilizada com outra requisição");
            }

            try {

                return objectMapper.readValue(
                        existente.getRespostaJson(),
                        ContaResponse.class
                );
            } catch (Exception e) {

                throw new IllegalStateException("Não foi possível recuperar a resposta da idempotência", e);
            }
        }

        if (valorCentavos <= 0) {
            throw new IllegalArgumentException("Valor do depósito deve ser maior que zero");
        }

        if (valorCentavos > 1_000_000) {
            throw new IllegalArgumentException("Valor do depósito não pode ser superior a R$ 10.000,00");
        }

        Conta conta = contaRepository.findByNumeroForUpdate(numeroConta).orElseThrow(() ->
                        new IllegalArgumentException("Conta não encontrada")
                );

        if (conta.getEstado() == EstadoConta.ENCERRADA) {
            throw new IllegalStateException("Não é possível depositar em conta encerrada");
        }

        conta.setSaldoCentavos(conta.getSaldoCentavos() + valorCentavos);

        movimentoService.criarMovimento(
                conta,
                null,
                TipoMovimento.ENTRADA,
                valorCentavos
        );

        ContaResponse response = new ContaResponse(conta);

        try {

            String respostaJson = objectMapper.writeValueAsString(response);

            idempotenciaService.salvarResposta(
                    endpoint,
                    chave,
                    respostaJson,
                    201
            );

        } catch (Exception e) {

            throw new IllegalStateException("Não foi possível salvar a resposta da idempotência", e);
        }

        return response;
    }
}