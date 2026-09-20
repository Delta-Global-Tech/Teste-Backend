package com.marlonmachado.desafiotecnico.services;

import com.marlonmachado.desafiotecnico.dto.MovementDTO;
import com.marlonmachado.desafiotecnico.dto.StatementDTO;
import com.marlonmachado.desafiotecnico.dto.SummaryDTO;
import com.marlonmachado.desafiotecnico.entities.Account;
import com.marlonmachado.desafiotecnico.entities.Movement;
import com.marlonmachado.desafiotecnico.entities.enums.AccountStatus;
import com.marlonmachado.desafiotecnico.entities.enums.ScheduleStatus;
import com.marlonmachado.desafiotecnico.exceptions.BusinessException;
import com.marlonmachado.desafiotecnico.projections.TransferWindowSummary;
import com.marlonmachado.desafiotecnico.repositories.AccountRepository;
import com.marlonmachado.desafiotecnico.repositories.MovementRepository;
import com.marlonmachado.desafiotecnico.repositories.ScheduleRepository;
import com.marlonmachado.desafiotecnico.repositories.TransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public StatementDTO getStatement(String numeroConta, Instant de, Instant ate, int page, int size) {

        Account conta = accountRepository.findByNumber(numeroConta)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "Conta não encontrada: " + numeroConta));

        Pageable paginacao = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sequence"));

        Page<Movement> movimentos;
        if (de != null && ate != null) {
            movimentos = movementRepository.findByAccountIdAndCreatedAtBetween(conta.getId(), de, ate, paginacao);
        } else {
            movimentos = movementRepository.findByAccountId(conta.getId(), paginacao);
        }

        List<MovementDTO> linhas = movimentos.stream()
                .map(m -> new MovementDTO(
                        m.getId(),
                        m.getType().name(),
                        centavosParaReais(m.getAmountCents()),
                        centavosParaReais(m.getBalanceAfterCents()),
                        m.getCreatedAt()
                ))
                .toList();

        return new StatementDTO(
                numeroConta,
                centavosParaReais(conta.getBalanceCents()),
                linhas);
    }

    @Transactional(readOnly = true)
    public SummaryDTO getSummary(int janelaMinutos) {

        Instant inicioJanela = Instant.now().minus(janelaMinutos, ChronoUnit.MINUTES);

        long contasAtivas = accountRepository.countByAccountStatus(AccountStatus.ACTIVE);
        Long saldoTotalUsuariosCentavos = accountRepository.sumBalanceOfUserAccounts();
        long agendamentosPendentes = scheduleRepository.countByState(ScheduleStatus.SCHEDULED);

        TransferWindowSummary resumoJanela = transferRepository.summarizeWindow(inicioJanela);

        Long somaMovimentosCentavos = movementRepository.sumAllMovements();

        return new SummaryDTO(
                contasAtivas,
                centavosParaReais(saldoTotalUsuariosCentavos),
                agendamentosPendentes,
                resumoJanela.getTotalTransferencias(),
                centavosParaReais(resumoJanela.getValorTransferido()),
                centavosParaReais(resumoJanela.getTaxas()),
                mediaCentavosParaReais(resumoJanela.getTicketMedio()),
                resumoJanela.getTotalEstornos(),
                somaMovimentosCentavos != null && somaMovimentosCentavos == 0L
        );
    }

    private BigDecimal centavosParaReais(Long centavos) {
        if (centavos == null) {
            return BigDecimal.ZERO.setScale(2);
        }

        return BigDecimal.valueOf(centavos, 2);
    }

    private BigDecimal mediaCentavosParaReais(Number mediaEmCentavos) {
        if (mediaEmCentavos == null) {
            return BigDecimal.ZERO.setScale(2);
        }

        return new BigDecimal(mediaEmCentavos.toString())
                .movePointLeft(2)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
