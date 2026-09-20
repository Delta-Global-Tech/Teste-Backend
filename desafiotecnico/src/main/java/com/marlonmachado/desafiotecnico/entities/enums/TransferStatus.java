package com.marlonmachado.desafiotecnico.entities.enums;

public enum TransferStatus {
    CREATED, CONFIRMED, REVERSED, FAILED;

    public boolean canTransitionTo(TransferStatus target) {
        return switch (this) {
            case CREATED -> target == CONFIRMED || target == FAILED;
            case CONFIRMED -> target == REVERSED;
            case REVERSED, FAILED -> false;
        };
    }
}
