package com.marlonmachado.desafiotecnico.integration;

import com.marlonmachado.desafiotecnico.repositories.AccountRepository;
import com.marlonmachado.desafiotecnico.repositories.MovementRepository;
import com.marlonmachado.desafiotecnico.repositories.TransferRepository;
import com.marlonmachado.desafiotecnico.services.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;


@Testcontainers
@SpringBootTest
public class ApplicationIntegrationTest extends DatabaseIntegrationTest{

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveSubirAplicacaoComPostgresReal() {
        assertTrue(postgres.isRunning());
    }

    @Test
    void deveConectarAplicacaoAoPostgresDoContainer() {
        Integer resultado = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        assertEquals(1, resultado);
    }

    @Test
    void deveCriarTabelasPelasMigrations() {
        Integer quantidade = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                      'users',
                      'accounts',
                      'transfers',
                      'movements',
                      'scheduled',
                      'idempotency_records'
                )
                """,
                Integer.class
        );

        assertEquals(6, quantidade);
    }


    @Test
    void deveCarregarContasIniciais() {
        Integer quantidade = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts",
                Integer.class
        );

        assertEquals(9, quantidade);
    }
}
