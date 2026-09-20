package com.marlonmachado.desafiotecnico.services;

import com.marlonmachado.desafiotecnico.dto.StatementDTO;
import com.marlonmachado.desafiotecnico.dto.SummaryDTO;
import com.marlonmachado.desafiotecnico.entities.Account;
import com.marlonmachado.desafiotecnico.entities.Movement;
import com.marlonmachado.desafiotecnico.entities.enums.AccountStatus;
import com.marlonmachado.desafiotecnico.exceptions.BusinessException;
import com.marlonmachado.desafiotecnico.projections.TransferWindowSummary;
import com.marlonmachado.desafiotecnico.repositories.AccountRepository;
import com.marlonmachado.desafiotecnico.repositories.MovementRepository;
import com.marlonmachado.desafiotecnico.repositories.ScheduleRepository;
import com.marlonmachado.desafiotecnico.repositories.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TransferWindowSummary transferWindowSummary;

    @InjectMocks
    private AccountService accountService;

    private Account conta;

    @BeforeEach
    void setUp() {
        conta = new Account();
        conta.setId(1L);
        conta.setNumber("CONTA-001");
        conta.setBalanceCents(100_000L);
        conta.setDailyLimitCents(200_000L);
        conta.setAccountStatus(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Deve retornar o extrato da conta sem filtro de datas")
    void deveRetornarExtratoSemFiltroDeDatas() {
        Movement movement = mockMovement(
                10L,
                "CREDIT",
                25_000L,
                125_000L
        );

        when(accountRepository.findByNumber("CONTA-001"))
                .thenReturn(Optional.of(conta));

        when(movementRepository.findByAccountId(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(movement)));

        StatementDTO response = accountService.getStatement(
                "CONTA-001",
                null,
                null,
                0,
                20
        );

        assertNotNull(response);
        assertEquals("CONTA-001", response.numeroConta());
        assertEquals(new BigDecimal("1000.00"), response.saldoAtual());
        assertEquals(1, response.movimentos().size());

        assertEquals(10L, response.movimentos().get(0).id());
        assertEquals("CREDIT", response.movimentos().get(0).tipo());
        assertEquals( new BigDecimal("250.00"), response.movimentos().get(0).valor());
        assertEquals(new BigDecimal("1250.00"), response.movimentos().get(0).saldoApos());

        verify(movementRepository).findByAccountId(
                eq(1L),
                any(Pageable.class)
        );

        verify(movementRepository, never())
                .findByAccountIdAndCreatedAtBetween(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("Deve retornar o extrato filtrado por período")
    void deveRetornarExtratoComFiltroDeDatas() {
        Instant inicio = Instant.parse("2026-09-01T00:00:00Z");
        Instant fim = Instant.parse("2026-09-19T23:59:59Z");

        Movement movement = mockMovement(
                20L,
                "DEBIT",
                10_000L,
                90_000L
        );

        when(accountRepository.findByNumber("CONTA-001"))
                .thenReturn(Optional.of(conta));

        when(movementRepository.findByAccountIdAndCreatedAtBetween(
                eq(1L),
                eq(inicio),
                eq(fim),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(movement)));

        StatementDTO response = accountService.getStatement(
                "CONTA-001",
                inicio,
                fim,
                0,
                20
        );

        assertNotNull(response);
        assertEquals("CONTA-001", response.numeroConta());
        assertEquals(1, response.movimentos().size());
        assertEquals(20L, response.movimentos().get(0).id());
        assertEquals("DEBIT", response.movimentos().get(0).tipo());
        assertEquals(new BigDecimal("100.00"), response.movimentos().get(0).valor());
        assertEquals(new BigDecimal("900.00"), response.movimentos().get(0).saldoApos());

        verify(movementRepository).findByAccountIdAndCreatedAtBetween(
                eq(1L),
                eq(inicio),
                eq(fim),
                any(Pageable.class)
        );

        verify(movementRepository, never())
                .findByAccountId(
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("Deve lançar exceção quando a conta não existir")
    void deveLancarExcecaoQuandoContaNaoExistir() {
        when(accountRepository.findByNumber("CONTA-999"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> accountService.getStatement(
                        "CONTA-999",
                        null,
                        null,
                        0,
                        20
                )
        );

        assertEquals(
                "RECURSO_NAO_ENCONTRADO",
                exception.getCodigo()
        );

        assertEquals(
                "Conta não encontrada: CONTA-999",
                exception.getMessage()
        );

        verify(movementRepository, never())
                .findByAccountId(any(), any());

        verify(movementRepository, never())
                .findByAccountIdAndCreatedAtBetween(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("Deve retornar o resumo do sistema")
    void deveRetornarResumoDoSistema() {
        when(accountRepository.countByAccountStatus(AccountStatus.ACTIVE))
                .thenReturn(5L);

        when(accountRepository.sumBalanceOfUserAccounts())
                .thenReturn(485_000L);

        when(scheduleRepository.countByState(any()))
                .thenReturn(3L);

        when(transferRepository.summarizeWindow(any(Instant.class)))
                .thenReturn(transferWindowSummary);

        when(transferWindowSummary.getTotalTransferencias())
                .thenReturn(12L);

        when(transferWindowSummary.getValorTransferido())
                .thenReturn(342_000L);

        when(transferWindowSummary.getTaxas())
                .thenReturn(1_299L);

        when(transferWindowSummary.getTicketMedio()).thenReturn(new BigDecimal("28500"));

        when(transferWindowSummary.getTotalEstornos())
                .thenReturn(1L);

        when(movementRepository.sumAllMovements())
                .thenReturn(0L);

        SummaryDTO response = accountService.getSummary(60);

        assertNotNull(response);

        assertEquals(5L, response.contasAtivasAgora());
        assertEquals(new BigDecimal("4850.00"), response.saldoTotalUsuariosAgora());
        assertEquals(3L, response.agendamentosPendentesAgora());
        assertEquals(12L, response.transferenciasNaJanela());
        assertEquals(new BigDecimal("3420.00"), response.valorTransferidoNaJanela());
        assertEquals(new BigDecimal("12.99"), response.taxasNaJanela());
        assertEquals(new BigDecimal("285.00"), response.ticketMedioNaJanela());
        assertEquals(1L, response.estornosNaJanela());
        assertEquals(true, response.somaDosMovimentosEhZero());

        verify(accountRepository)
                .countByAccountStatus(AccountStatus.ACTIVE);

        verify(accountRepository)
                .sumBalanceOfUserAccounts();

        verify(scheduleRepository)
                .countByState(any());

        verify(transferRepository)
                .summarizeWindow(any(Instant.class));

        verify(movementRepository)
                .sumAllMovements();
    }

    @Test
    @DisplayName("Deve retornar zero quando o saldo total de usuários for nulo")
    void deveRetornarZeroQuandoSaldoTotalForNulo() {
        when(accountRepository.countByAccountStatus(AccountStatus.ACTIVE))
                .thenReturn(0L);

        when(accountRepository.sumBalanceOfUserAccounts())
                .thenReturn(null);

        when(scheduleRepository.countByState(any()))
                .thenReturn(0L);

        when(transferRepository.summarizeWindow(any(Instant.class)))
                .thenReturn(transferWindowSummary);

        when(transferWindowSummary.getTotalTransferencias())
                .thenReturn(0L);

        when(transferWindowSummary.getValorTransferido())
                .thenReturn(0L);

        when(transferWindowSummary.getTaxas())
                .thenReturn(0L);

        when(transferWindowSummary.getTicketMedio())
                .thenReturn(null);

        when(transferWindowSummary.getTotalEstornos())
                .thenReturn(0L);

        when(movementRepository.sumAllMovements())
                .thenReturn(null);

        SummaryDTO response = accountService.getSummary(60);

        assertNotNull(response);
        assertEquals(0L, response.contasAtivasAgora());
        assertEquals(BigDecimal.ZERO.setScale(2), response.saldoTotalUsuariosAgora());
        assertEquals(0L, response.agendamentosPendentesAgora());
        assertEquals(0L, response.transferenciasNaJanela());
        assertEquals(BigDecimal.ZERO.setScale(2), response.valorTransferidoNaJanela());
        assertEquals(BigDecimal.ZERO.setScale(2), response.taxasNaJanela());
        assertEquals(BigDecimal.ZERO.setScale(2), response.ticketMedioNaJanela());
        assertEquals(0L, response.estornosNaJanela());
        assertEquals(false, response.somaDosMovimentosEhZero());
    }

    private Movement mockMovement(
            Long id,
            String type,
            Long amountCents,
            Long balanceAfterCents
    ) {
        Movement movement = org.mockito.Mockito.mock(Movement.class);

        when(movement.getId()).thenReturn(id);
        when(movement.getType()).thenReturn(
                Enum.valueOf(
                        com.marlonmachado.desafiotecnico.entities.enums.MovementType.class,
                        type
                )
        );
        when(movement.getAmountCents()).thenReturn(amountCents);
        when(movement.getBalanceAfterCents()).thenReturn(balanceAfterCents);
        when(movement.getCreatedAt()).thenReturn(
                Instant.parse("2026-09-19T12:00:00Z")
        );

        return movement;
    }
}
