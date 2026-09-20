package com.marlonmachado.desafiotecnico.services;

import com.marlonmachado.desafiotecnico.dto.TransferRequestDTO;
import com.marlonmachado.desafiotecnico.entities.Account;
import com.marlonmachado.desafiotecnico.entities.Transfer;
import com.marlonmachado.desafiotecnico.entities.enums.AccountStatus;
import com.marlonmachado.desafiotecnico.entities.enums.TransferStatus;
import com.marlonmachado.desafiotecnico.exceptions.BusinessException;
import com.marlonmachado.desafiotecnico.repositories.AccountRepository;
import com.marlonmachado.desafiotecnico.repositories.MovementRepository;
import com.marlonmachado.desafiotecnico.repositories.TransferRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceDailyLimitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private FeeCalculator feeCalculator;

    @InjectMocks
    private TransferService transferService;

    private Account contaOrigem;
    private Account contaDestino;
    private Account sistemaTaxas;
    private TransferRequestDTO request;

    @BeforeEach
    void setUp() {
        contaOrigem = new Account();
        contaOrigem.setId(1L);
        contaOrigem.setNumber("CONTA-001");
        contaOrigem.setBalanceCents(200_000L);
        contaOrigem.setDailyLimitCents(100_000L);
        contaOrigem.setAccountStatus(AccountStatus.ACTIVE);

        contaDestino = new Account();
        contaDestino.setId(2L);
        contaDestino.setNumber("CONTA-002");
        contaDestino.setBalanceCents(50_000L);
        contaDestino.setDailyLimitCents(100_000L);
        contaDestino.setAccountStatus(AccountStatus.ACTIVE);

        sistemaTaxas = new Account();
        sistemaTaxas.setId(3L);
        sistemaTaxas.setNumber("SISTEMA-TAXAS");
        sistemaTaxas.setBalanceCents(0L);
        sistemaTaxas.setDailyLimitCents(0L);
        sistemaTaxas.setAccountStatus(AccountStatus.ACTIVE);

        request = new TransferRequestDTO(
                "CONTA-001",
                "CONTA-002",
                new BigDecimal("500.00")
        );

        /*
         * O service busca os IDs da origem e do destino antes de
         * adquirir os locks pessimistas.
         */
        when(accountRepository.findIdByAccountNumber("CONTA-001"))
                .thenReturn(Optional.of(contaOrigem.getId()));

        when(accountRepository.findIdByAccountNumber("CONTA-002"))
                .thenReturn(Optional.of(contaDestino.getId()));

        /*
         * O service bloqueia as entidades pela ordem dos IDs.
         */
        when(accountRepository.findByIdForUpdate(any(Long.class)))
                .thenAnswer(invocation -> {
                    Long id = invocation.getArgument(0);

                    if (id.equals(contaOrigem.getId())) {
                        return Optional.of(contaOrigem);
                    }

                    if (id.equals(contaDestino.getId())) {
                        return Optional.of(contaDestino);
                    }

                    if (id.equals(sistemaTaxas.getId())) {
                        return Optional.of(sistemaTaxas);
                    }

                    return Optional.empty();
                });

        /*
         * O service calcula a taxa antes de bloquear as contas.
         * Por padrão, os testes usam transferência sem taxa.
         */
        when(feeCalculator.calculate(any(Long.class)))
                .thenReturn(0L);

        /*
         * Nenhuma chave de idempotência foi utilizada antes.
         */
        when(idempotencyService.buscarRegistroExistente(
                anyString(),
                anyString(),
                any()
        )).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Deve permitir transferência quando o limite diário não for excedido")
    void devePermitirTransferenciaDentroDoLimite() {
        when(transferRepository.sumDebitedToday(
                eq(contaOrigem.getId()),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(0L);

        configurarPersistenciaDaTransferencia();

        assertDoesNotThrow(() ->
                transferService.transfer(
                        request.contaOrigem(),
                        request.contaDestino(),
                        request.valor(),
                        "chave-001"
                )
        );

        assertEquals(
                150_000L,
                contaOrigem.getBalanceCents()
        );

        assertEquals(
                100_000L,
                contaDestino.getBalanceCents()
        );
    }

    @Test
    @DisplayName("Deve rejeitar transferência quando o limite diário for excedido")
    void deveRejeitarQuandoLimiteDiarioForExcedido() {
        when(transferRepository.sumDebitedToday(
                eq(contaOrigem.getId()),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(60_000L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(
                        request.contaOrigem(),
                        request.contaDestino(),
                        request.valor(),
                        "chave-003"
                )
        );

        assertEquals(
                "LIMITE_DIARIO_EXCEDIDO",
                exception.getCodigo()
        );

        assertEquals(
                "Limite diário de transferência excedido",
                exception.getMessage()
        );

        /*
         * R$ 600,00 já utilizados + R$ 500,00 da nova transferência
         * ultrapassam o limite de R$ 1.000,00.
         */
        assertEquals(
                200_000L,
                contaOrigem.getBalanceCents()
        );

        assertEquals(
                50_000L,
                contaDestino.getBalanceCents()
        );
    }

    @Test
    @DisplayName("A taxa não deve ser considerada no limite diário")
    void taxaNaoDeveSerConsideradaNoLimiteDiario() {
        contaOrigem.setDailyLimitCents(100_001L);

        request = new TransferRequestDTO(
                "CONTA-001",
                "CONTA-002",
                new BigDecimal("100.01")
        );

        /*
         * R$ 100,01 = 10.001 centavos.
         * A taxa esperada é R$ 1,00 = 100 centavos.
         */
        when(feeCalculator.calculate(10_001L))
                .thenReturn(100L);

        /*
         * 90.000 + 10.001 = 100.001.
         *
         * A operação deve ser aceita porque o limite considera
         * somente o valor transferido, e não a taxa.
         */
        when(transferRepository.sumDebitedToday(
                eq(contaOrigem.getId()),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(90_000L);

        /*
         * Neste ponto o service usa findByNumber para localizar
         * a conta SISTEMA-TAXAS.
         */
        when(accountRepository.findByNumber("SISTEMA-TAXAS"))
                .thenReturn(Optional.of(sistemaTaxas));

        configurarPersistenciaDaTransferencia();

        assertDoesNotThrow(() ->
                transferService.transfer(
                        request.contaOrigem(),
                        request.contaDestino(),
                        request.valor(),
                        "chave-004"
                )
        );

        /*
         * 200.000 - 10.001 - 100 = 189.899 centavos.
         */
        assertEquals(
                189_899L,
                contaOrigem.getBalanceCents()
        );

        assertEquals(
                60_001L,
                contaDestino.getBalanceCents()
        );

        assertEquals(
                100L,
                sistemaTaxas.getBalanceCents()
        );
    }

    private void configurarPersistenciaDaTransferencia() {
        when(movementRepository.findLastMovementByAccountId(
                any(Long.class)
        )).thenReturn(Optional.empty());

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer transferencia =
                            invocation.getArgument(0);

                    transferencia.setId(100L);
                    return transferencia;
                });
    }
}