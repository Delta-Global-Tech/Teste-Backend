package com.marlonmachado.desafiotecnico.integration;


import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class PostgreSqlContainerOnlyTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("desafiotecnico-postgres")
                    .withUsername("desafiotecnico")
                    .withPassword("desafiotecnico");

    @Test
    void deveIniciarPostgres() {
        assertTrue(postgres.isRunning());
    }
}