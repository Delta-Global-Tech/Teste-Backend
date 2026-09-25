

-- EXTENSÕES
-- ---------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------
-- TABELA: usuario
-- ---------------------------------------------------------------
CREATE TABLE usuario (
    id         BIGSERIAL    PRIMARY KEY,
    nome       VARCHAR(150) NOT NULL,
    cpf        VARCHAR(11)  NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuario_cpf UNIQUE (cpf)
);

-- ---------------------------------------------------------------
-- TABELA: conta
-- ---------------------------------------------------------------
CREATE TABLE conta (
    id                     BIGSERIAL    PRIMARY KEY,
    numero                 VARCHAR(50)  NOT NULL,
    usuario_id             BIGINT       REFERENCES usuario(id),   -- NULL para contas do sistema
    saldo_centavos         BIGINT       NOT NULL DEFAULT 0,
    limite_diario_centavos BIGINT       NOT NULL DEFAULT 200000,  -- R$ 2.000,00
    estado                 VARCHAR(20)  NOT NULL DEFAULT 'ATIVA',
    versao                 BIGINT       NOT NULL DEFAULT 0,        -- optimistic locking
    criada_em              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_conta_numero    UNIQUE (numero),
    CONSTRAINT ck_conta_estado    CHECK (estado IN ('ATIVA', 'BLOQUEADA', 'ENCERRADA')),
    CONSTRAINT ck_conta_saldo_nao_negativo
        CHECK (
            -- Contas do sistema (usuario_id IS NULL) podem ser negativas
            usuario_id IS NULL
            OR saldo_centavos >= 0
        ),
    CONSTRAINT ck_limite_diario_positivo CHECK (limite_diario_centavos > 0)
);

-- ---------------------------------------------------------------
-- TABELA: transferencia
-- ---------------------------------------------------------------
CREATE TABLE transferencia (
    id                       BIGSERIAL   PRIMARY KEY,
    conta_origem_id          BIGINT      NOT NULL REFERENCES conta(id),
    conta_destino_id         BIGINT      NOT NULL REFERENCES conta(id),
    valor_centavos           BIGINT      NOT NULL,
    taxa_centavos            BIGINT      NOT NULL DEFAULT 0,
    estado                   VARCHAR(20) NOT NULL DEFAULT 'CRIADA',
    transferencia_original_id BIGINT     REFERENCES transferencia(id),  -- preenchido no estorno
    criada_em                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    concluida_em             TIMESTAMPTZ,
    CONSTRAINT ck_transferencia_estado
        CHECK (estado IN ('CRIADA', 'CONFIRMADA', 'FALHADA', 'ESTORNADA')),
    CONSTRAINT ck_transferencia_valor_positivo   CHECK (valor_centavos > 0),
    CONSTRAINT ck_transferencia_taxa_nao_negativa CHECK (taxa_centavos >= 0),
    CONSTRAINT ck_transferencia_contas_diferentes
        CHECK (conta_origem_id <> conta_destino_id)
);

-- ---------------------------------------------------------------
-- TABELA: movimento  (IMUTÁVEL — protegida por trigger)
-- ---------------------------------------------------------------
CREATE TABLE movimento (
    id               BIGSERIAL   PRIMARY KEY,
    conta_id         BIGINT      NOT NULL REFERENCES conta(id),
    transferencia_id BIGINT      REFERENCES transferencia(id),
    sequencia        BIGINT      NOT NULL,   -- sequência por conta, começa em 1
    tipo             VARCHAR(10) NOT NULL,
    valor_centavos   BIGINT      NOT NULL,
    saldo_apos_centavos BIGINT   NOT NULL,
    criado_em        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_movimento_conta_sequencia UNIQUE (conta_id, sequencia),
    CONSTRAINT ck_movimento_tipo     CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_movimento_valor_positivo CHECK (valor_centavos > 0)
);

-- ---------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_movimento_imutavel()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'MOVIMENTO_IMUTAVEL: UPDATE não permitido na tabela movimento (id=%)', OLD.id;
    ELSIF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'MOVIMENTO_IMUTAVEL: DELETE não permitido na tabela movimento (id=%)', OLD.id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_movimento_no_update
    BEFORE UPDATE ON movimento
    FOR EACH ROW EXECUTE FUNCTION fn_movimento_imutavel();

CREATE TRIGGER tg_movimento_no_delete
    BEFORE DELETE ON movimento
    FOR EACH ROW EXECUTE FUNCTION fn_movimento_imutavel();

-- ---------------------------------------------------------------
-- TABELA: agendamento
-- ---------------------------------------------------------------
CREATE TABLE agendamento (
    id               BIGSERIAL   PRIMARY KEY,
    conta_origem_id  BIGINT      NOT NULL REFERENCES conta(id),
    conta_destino_id BIGINT      NOT NULL REFERENCES conta(id),
    valor_centavos   BIGINT      NOT NULL,
    executar_em      TIMESTAMPTZ NOT NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    tentativas       INT         NOT NULL DEFAULT 0,
    transferencia_id BIGINT      REFERENCES transferencia(id),
    criado_em        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processando_em   TIMESTAMPTZ,             -- quando o job pegou para processar
    CONSTRAINT ck_agendamento_estado
        CHECK (estado IN ('PENDENTE', 'EXECUTADO', 'FALHADO', 'CANCELADO')),
    CONSTRAINT ck_agendamento_valor_positivo CHECK (valor_centavos > 0)
);

-- ---------------------------------------------------------------
-- TABELA: idempotencia
-- ---------------------------------------------------------------
CREATE TABLE idempotencia (
    id              BIGSERIAL    PRIMARY KEY,
    chave           VARCHAR(255) NOT NULL,
    endpoint        VARCHAR(255) NOT NULL,
    hash_requisicao VARCHAR(64)  NOT NULL,   -- SHA-256 hex do body
    resposta_json   TEXT         NOT NULL,
    status_http     INT          NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotencia_endpoint_chave UNIQUE (endpoint, chave)
);

-- ---------------------------------------------------------------
-- ÍNDICES de performance
-- ---------------------------------------------------------------

-- conta.numero — usado em todos os endpoints que recebem número da conta
CREATE INDEX idx_conta_numero ON conta(numero);

-- conta.estado — usado no resumo (contagem de contas ativas)
CREATE INDEX idx_conta_estado ON conta(estado);

-- movimento: extrato por conta, ordenado por sequência
CREATE INDEX idx_movimento_conta_seq ON movimento(conta_id, sequencia DESC);

-- movimento: extrato por conta + data (filtros de período)
CREATE INDEX idx_movimento_conta_data ON movimento(conta_id, criado_em DESC);

-- movimento: verificação de consistência (soma zero)
CREATE INDEX idx_movimento_transferencia ON movimento(transferencia_id);

-- transferência: consultas de limite diário (saídas da conta origem num dia)
CREATE INDEX idx_transferencia_origem_criada ON transferencia(conta_origem_id, criada_em);

-- transferência: consultas de destino (extrato relacionado)
CREATE INDEX idx_transferencia_destino ON transferencia(conta_destino_id);

-- transferência: janela de tempo para o resumo
CREATE INDEX idx_transferencia_criada_em ON transferencia(criada_em);

-- transferência: estado (para estornos, falhadas)
CREATE INDEX idx_transferencia_estado ON transferencia(estado);

-- agendamento: job busca pendentes com executar_em vencido
CREATE INDEX idx_agendamento_estado_executar ON agendamento(estado, executar_em);

-- idempotencia: lookup por endpoint+chave
CREATE INDEX idx_idempotencia_endpoint_chave ON idempotencia(endpoint, chave);


COMMENT ON TABLE movimento IS 'Registro imutável de todas as alterações de saldo. Protegido por trigger contra UPDATE e DELETE.';
COMMENT ON COLUMN movimento.sequencia IS 'Sequência monotônica por conta. Começa em 1. Única por conta. Não pode pular.';
COMMENT ON COLUMN conta.versao IS 'Versão para optimistic locking via @Version do Hibernate.';
COMMENT ON COLUMN conta.saldo_centavos IS 'Saldo em centavos (BIGINT). Nunca usar float/double.';
COMMENT ON COLUMN transferencia.transferencia_original_id IS 'Preenchido apenas no estorno. Aponta para a transferência original que foi estornada.';
COMMENT ON COLUMN agendamento.processando_em IS 'Timestamp de quando o job adquiriu o lock. Usado para detectar agendamentos travados (timeout).';
