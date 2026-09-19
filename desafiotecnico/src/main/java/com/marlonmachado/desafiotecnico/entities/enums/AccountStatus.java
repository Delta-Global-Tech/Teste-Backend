package com.marlonmachado.desafiotecnico.entities.enums;

public enum AccountStatus {
    ACTIVE, BLOCKED, CLOSED;

    public boolean canSend() {
        return this == ACTIVE;
    }

    public boolean canTransitionTo(AccountStatus target) {
        return switch (this) {
            case ACTIVE -> target == BLOCKED || target == CLOSED;
            case BLOCKED -> target == ACTIVE || target == CLOSED;
            case CLOSED -> false;
        };
    }
}
