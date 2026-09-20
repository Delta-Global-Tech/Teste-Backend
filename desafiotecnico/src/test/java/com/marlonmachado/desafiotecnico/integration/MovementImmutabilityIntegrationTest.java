package com.marlonmachado.desafiotecnico.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class MovementImmutabilityIntegrationTest
        extends DatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Banco deve rejeitar UPDATE em movimento existente")
    void bancoDeveRejeitarUpdateEmMovimento() {
        Long movementId = buscarMovementId();

        assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        """
                        UPDATE movements
                        SET amount_cents = ?
                        WHERE id = ?
                        """,
                        999_999L,
                        movementId
                )
        );
    }

    @Test
    @DisplayName("Banco deve rejeitar DELETE de movimento existente")
    void bancoDeveRejeitarDeleteDeMovimento() {
        Long movementId = buscarMovementId();

        assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        """
                        DELETE FROM movements
                        WHERE id = ?
                        """,
                        movementId
                )
        );
    }

    private Long buscarMovementId() {
        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM movements
                ORDER BY id
                LIMIT 1
                """,
                Long.class
        );
    }
}