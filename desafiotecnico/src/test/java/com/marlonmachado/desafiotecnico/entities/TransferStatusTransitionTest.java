package com.marlonmachado.desafiotecnico.entities;

import com.marlonmachado.desafiotecnico.entities.enums.TransferStatus;
import com.marlonmachado.desafiotecnico.exceptions.IllegalStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransferStatusTransitionTest {

    @Test
    @DisplayName("Deve permitir CREATED para CONFIRMED")
    void deveConfirmarTransferenciaCriada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.CREATED);

        Instant agora = Instant.now();

        transfer.confirm(agora);

        assertEquals(TransferStatus.CONFIRMED, transfer.getStatus());
        assertEquals(agora, transfer.getCompletedAt());
    }

    @Test
    @DisplayName("Deve permitir CREATED para FAILED")
    void deveFalharTransferenciaCriada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.CREATED);

        transfer.fail();

        assertEquals(TransferStatus.FAILED, transfer.getStatus());
    }

    @Test
    @DisplayName("Deve permitir CONFIRMED para REVERSED")
    void deveEstornarTransferenciaConfirmada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.CONFIRMED);

        transfer.reverse();

        assertEquals(TransferStatus.REVERSED, transfer.getStatus());
    }

    @Test
    @DisplayName("Não deve confirmar transferência já confirmada")
    void naoDeveConfirmarTransferenciaJaConfirmada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.CONFIRMED);

        assertThrows(
                IllegalStateTransitionException.class,
                () -> transfer.confirm(Instant.now())
        );

        assertEquals(TransferStatus.CONFIRMED, transfer.getStatus());
    }

    @Test
    @DisplayName("Não deve falhar transferência confirmada")
    void naoDeveFalharTransferenciaConfirmada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.CONFIRMED);

        assertThrows(
                IllegalStateTransitionException.class,
                transfer::fail
        );

        assertEquals(TransferStatus.CONFIRMED, transfer.getStatus());
    }

    @Test
    @DisplayName("Não deve estornar transferência criada")
    void naoDeveEstornarTransferenciaCriada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.CREATED);

        assertThrows(
                IllegalStateTransitionException.class,
                transfer::reverse
        );

        assertEquals(TransferStatus.CREATED, transfer.getStatus());
    }

    @Test
    @DisplayName("Não deve confirmar transferência falhada")
    void naoDeveConfirmarTransferenciaFalhada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.FAILED);

        assertThrows(
                IllegalStateTransitionException.class,
                () -> transfer.confirm(Instant.now())
        );

        assertEquals(TransferStatus.FAILED, transfer.getStatus());
    }

    @Test
    @DisplayName("Não deve estornar transferência falhada")
    void naoDeveEstornarTransferenciaFalhada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.FAILED);

        assertThrows(
                IllegalStateTransitionException.class,
                transfer::reverse
        );

        assertEquals(TransferStatus.FAILED, transfer.getStatus());
    }

    @Test
    @DisplayName("Não deve estornar transferência já estornada")
    void naoDeveEstornarTransferenciaJaEstornada() {
        Transfer transfer = new Transfer();
        transfer.setStatus(TransferStatus.REVERSED);

        assertThrows(
                IllegalStateTransitionException.class,
                transfer::reverse
        );

        assertEquals(TransferStatus.REVERSED, transfer.getStatus());
    }
}
