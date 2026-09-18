CREATE TABLE users(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    cpf  VARCHAR(11)  NOT NULL,
    CONSTRAINT uk_users_cpf UNIQUE (cpf)
);

CREATE TABLE accounts(
    id                BIGSERIAL PRIMARY KEY,
    account_number    VARCHAR(30) NOT NULL,
    balance_cents     BIGINT      NOT NULL,
    daily_limit_cents BIGINT      NOT NULL,
    account_status    VARCHAR(15) NOT NULL,
    user_id           BIGINT NULL REFERENCES users(id),
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_accounts_number UNIQUE (account_number)
);

CREATE TABLE transfers(
    id                     BIGSERIAL PRIMARY KEY,
    source_account_id      BIGINT      NOT NULL REFERENCES accounts (id),
    destination_account_id BIGINT      NOT NULL REFERENCES accounts (id),
    amount_cents           BIGINT      NOT NULL,
    fee_cents              BIGINT      NOT NULL,
    status                 VARCHAR(15) NOT NULL,
    original_transfer_id   BIGINT NULL REFERENCES transfers(id),
    created_at             TIMESTAMPTZ NOT NULL,
    completed_at           TIMESTAMPTZ NULL,
    CONSTRAINT uk_transfers_original_reversal UNIQUE (original_transfer_id)
);

CREATE TABLE movements(
    id                  BIGSERIAL PRIMARY KEY,
    account_id          BIGINT      NOT NULL REFERENCES accounts (id),
    transfer_id         BIGINT      NOT NULL REFERENCES transfers (id),
    sequence_number     BIGINT      NOT NULL,
    type                VARCHAR(10) NOT NULL,
    amount_cents        BIGINT      NOT NULL,
    balance_after_cents BIGINT      NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_movements_account_sequence UNIQUE (account_id, sequence_number)
);

CREATE TABLE scheduled(
    id                     BIGSERIAL PRIMARY KEY,
    source_account_id      BIGINT      NOT NULL REFERENCES accounts (id),
    destination_account_id BIGINT      NOT NULL REFERENCES accounts (id),
    amount_cents           BIGINT      NOT NULL,
    execute_at             TIMESTAMPTZ NOT NULL,
    state                  VARCHAR(20) NOT NULL,
    attempts               INT         NOT NULL DEFAULT 0,
    transfer_id            BIGINT NULL REFERENCES transfers(id)
);

CREATE TABLE idempotency_records(
    id              BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    endpoint        VARCHAR(100) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    response_json   JSONB        NOT NULL,
    http_status     INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_idempotency_endpoint_key UNIQUE (endpoint, idempotency_key)
);

CREATE INDEX idx_transfers_created_at ON transfers (created_at);
CREATE INDEX idx_transfers_status_created_at ON transfers (status, created_at);
CREATE INDEX idx_movements_account_sequence ON movements (account_id, sequence_number DESC);
CREATE INDEX idx_movements_account_created_at ON movements (account_id, created_at);
CREATE INDEX idx_movements_transfer ON movements (transfer_id);
CREATE INDEX idx_scheduled_due ON scheduled (state, execute_at);