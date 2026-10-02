CREATE TABLE usuario (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(30) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE
);

CREATE TABLE conta (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero VARCHAR(20) NOT NULL UNIQUE,
    usuario_id BIGINT REFERENCES usuario(id),
    saldo_centavos BIGINT NOT NULL DEFAULT 0,
    limite_diario_centavos BIGINT NOT NULL,
    estado VARCHAR(10) NOT NULL CHECK (
        estado IN ('ATIVA', 'BLOQUEADA', 'ENCERRADA')
        ),
    versao BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE transferencia (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conta_origem_id BIGINT NOT NULL REFERENCES conta(id),
    conta_destino_id BIGINT NOT NULL REFERENCES conta(id),
    valor_centavos BIGINT NOT NULL CHECK (valor_centavos > 0),
    taxa_centavos BIGINT NOT NULL DEFAULT 0 CHECK (taxa_centavos >= 0),
    estado VARCHAR(12) NOT NULL CHECK (
        estado IN ('CRIADA', 'CONFIRMADA', 'ESTORNADA', 'FALHADA')
        ),
    transferencia_original_id BIGINT REFERENCES transferencia(id),
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    concluida_em TIMESTAMPTZ
);

CREATE TABLE movimento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conta_id BIGINT NOT NULL REFERENCES conta(id),
    transferencia_id BIGINT REFERENCES transferencia(id),
    sequencia BIGINT NOT NULL,
    tipo VARCHAR(7) NOT NULL CHECK (
        tipo IN ('ENTRADA', 'SAIDA')
        ),
    valor_centavos BIGINT NOT NULL CHECK (valor_centavos > 0),
    saldo_apos_centavos BIGINT NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (conta_id, sequencia)
);

CREATE TABLE agendamento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conta_origem_id BIGINT NOT NULL REFERENCES conta(id),
    conta_destino_id BIGINT NOT NULL REFERENCES conta(id),
    valor_centavos BIGINT NOT NULL CHECK (valor_centavos > 0),
    executar_em TIMESTAMPTZ NOT NULL,
    estado VARCHAR(10) NOT NULL CHECK (
        estado IN ('AGENDADO', 'EXECUTADO', 'FALHADO')
        ),
    tentativas BIGINT NOT NULL DEFAULT 0,
    transferencia_id BIGINT REFERENCES transferencia(id)
);

CREATE TABLE idempotencia (
    chave TEXT NOT NULL,
    endpoint VARCHAR(100) NOT NULL,
    hash_requisicao TEXT NOT NULL,
    resposta_json JSONB,
    status_http INTEGER,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (endpoint, chave)
);