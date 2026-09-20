package com.marlonmachado.desafiotecnico.integration;

import com.marlonmachado.desafiotecnico.dto.TransferReceiptDTO;
import com.marlonmachado.desafiotecnico.services.TransferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;




@Testcontainers
class TransferConcurrencyIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void apenasUmaTransferenciaDeveVencerPorSaldo()
            throws Exception {

        long saldoInicial = saldoDaConta("CONTA-001");
        long transferenciasAntes = quantidadeTransferencias();

        assertEquals(100_000L, saldoInicial);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch barreiraInicio =
                new CountDownLatch(1);

        CountDownLatch tarefasProntas =
                new CountDownLatch(2);

        try {
            Callable<ResultadoTransferencia> tarefa1 =
                    criarTarefa(
                            barreiraInicio,
                            tarefasProntas,
                            "saldo-concorrente-001"
                    );

            Callable<ResultadoTransferencia> tarefa2 =
                    criarTarefa(
                            barreiraInicio,
                            tarefasProntas,
                            "saldo-concorrente-002"
                    );

            Future<ResultadoTransferencia> futuro1 =
                    executor.submit(tarefa1);

            Future<ResultadoTransferencia> futuro2 =
                    executor.submit(tarefa2);

            boolean tarefasAguardando =
                    tarefasProntas.await(5, TimeUnit.SECONDS);

            assertEquals(true, tarefasAguardando);

            barreiraInicio.countDown();

            ResultadoTransferencia resultado1 =
                    futuro1.get(10, TimeUnit.SECONDS);

            ResultadoTransferencia resultado2 =
                    futuro2.get(10, TimeUnit.SECONDS);

            List<ResultadoTransferencia> resultados =
                    List.of(resultado1, resultado2);

            long sucessos = resultados.stream()
                    .filter(ResultadoTransferencia::sucesso)
                    .count();

            long falhas = resultados.stream()
                    .filter(resultado -> !resultado.sucesso())
                    .count();

            assertEquals(1, sucessos);
            assertEquals(1, falhas);

            /*
             * R$ 1.000,00
             * - R$ 600,00 de transferência
             * - R$ 6,00 de taxa
             * = R$ 394,00
             */
            assertEquals(
                    39_400L,
                    saldoDaConta("CONTA-001")
            );

            assertEquals(
                    transferenciasAntes + 1,
                    quantidadeTransferencias()
            );

            assertEquals(
                    0L,
                    somaMovimentos()
            );

        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<ResultadoTransferencia> criarTarefa(
            CountDownLatch barreiraInicio,
            CountDownLatch tarefasProntas,
            String idempotencyKey
    ) {
        return () -> {
            tarefasProntas.countDown();

            barreiraInicio.await();

            try {
                TransferReceiptDTO resposta =
                        transferService.transfer(
                                "CONTA-001",
                                "CONTA-002",
                                new BigDecimal("600.00"),
                                idempotencyKey
                        );

                return ResultadoTransferencia.sucesso(resposta);

            } catch (Exception exception) {
                return ResultadoTransferencia.falha(exception);
            }
        };
    }

    private long saldoDaConta(String numeroConta) {
        return jdbcTemplate.queryForObject(
                """
                SELECT balance_cents
                FROM accounts
                WHERE account_number = ?
                """,
                Long.class,
                numeroConta
        );
    }

    private long quantidadeTransferencias() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM transfers
                """,
                Long.class
        );
    }

    private long somaMovimentos() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(
                    SUM(
                        CASE
                            WHEN type = 'CREDIT' THEN amount_cents
                            ELSE -amount_cents
                        END
                    ),
                    0
                )
                FROM movements
                """,
                Long.class
        );
    }

    private record ResultadoTransferencia(
            boolean sucesso,
            Exception exception
    ) {

        static ResultadoTransferencia sucesso(
                TransferReceiptDTO resposta
        ) {
            return new ResultadoTransferencia(true, null);
        }

        static ResultadoTransferencia falha(
                Exception exception
        ) {
            return new ResultadoTransferencia(false, exception);
        }
    }


}