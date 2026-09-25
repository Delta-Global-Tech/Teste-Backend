package com.financial.transfer.service;

import com.financial.transfer.dto.response.ExtratoResponse;
import com.financial.transfer.dto.response.MovimentoResponse;
import com.financial.transfer.entity.Conta;
import com.financial.transfer.exception.ApiException;
import com.financial.transfer.mapper.MovimentoMapper;
import com.financial.transfer.repository.ContaRepository;
import com.financial.transfer.repository.MovimentoRepository;
import com.financial.transfer.util.MoneyUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ExtratoService {

    private final ContaRepository    contaRepository;
    private final MovimentoRepository movimentoRepository;
    private final MovimentoMapper    movimentoMapper;

    public ExtratoService(ContaRepository contaRepository,
                          MovimentoRepository movimentoRepository,
                          MovimentoMapper movimentoMapper) {
        this.contaRepository    = contaRepository;
        this.movimentoRepository = movimentoRepository;
        this.movimentoMapper    = movimentoMapper;
    }

    @Transactional(readOnly = true)
    public ExtratoResponse obter(String numeroConta,
                                 OffsetDateTime de,
                                 OffsetDateTime ate,
                                 int page,
                                 int size) {

        Conta conta = contaRepository.findByNumero(numeroConta)
            .orElseThrow(() -> ApiException.contaNaoEncontrada(numeroConta));

        Page<com.financial.transfer.entity.Movimento> pagina;

        PageRequest pageable = PageRequest.of(page, size);

        if (de == null && ate == null) {
            pagina = movimentoRepository.findExtrato(
                conta.getId(),
                pageable
            );
        } else if (de != null && ate == null) {
            pagina = movimentoRepository.findExtratoDesde(
                conta.getId(),
                de,
                pageable
            );
        } else if (de == null) {
            pagina = movimentoRepository.findExtratoAte(
                conta.getId(),
                ate,
                pageable
            );
        } else {
            pagina = movimentoRepository.findExtratoEntre(
                conta.getId(),
                de,
                ate,
                pageable
            );
        }

        List<MovimentoResponse> movimentos = pagina.getContent()
            .stream()
            .map(movimentoMapper::toResponse)
            .toList();

        return new ExtratoResponse(
            numeroConta,
            MoneyUtil.centavosParaReais(conta.getSaldoCentavos()),
            pagina.getTotalElements(),
            page,
            size,
            pagina.getTotalPages(),
            movimentos
        );
    }}
