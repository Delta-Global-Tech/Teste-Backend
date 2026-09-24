-- SEQUENCIA passa a ser contador por conta (1, 2, 3...), calculado pela aplicação
-- enquanto segura o lock da linha da conta. A sequence global deixa de ser usada.
ALTER TABLE MOVIMENTO ALTER COLUMN SEQUENCIA DROP DEFAULT;
DROP SEQUENCE movimento_seq_gen;

-- Renumera os movimentos da carga inicial para começarem em 1 em cada conta.
UPDATE MOVIMENTO m
SET SEQUENCIA = r.NOVA
FROM (
    SELECT ID, ROW_NUMBER() OVER (PARTITION BY CONTA_ID ORDER BY SEQUENCIA) AS NOVA
    FROM MOVIMENTO
) r
WHERE r.ID = m.ID;

-- Rede de segurança: mesmo que o lock falhe, o banco não aceita sequência repetida.
ALTER TABLE MOVIMENTO ADD CONSTRAINT UK_MOVIMENTO_CONTA_SEQUENCIA UNIQUE (CONTA_ID, SEQUENCIA);

-- Diferencia o movimento de taxa do movimento principal (taxa não conta para o limite diário).
ALTER TABLE MOVIMENTO ADD COLUMN TAXA BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE MOVIMENTO ADD CONSTRAINT CK_MOVIMENTO_VALOR_POSITIVO CHECK (VALOR_CENTAVOS > 0);

-- Conta de usuário nunca fica negativa; contas do sistema (sem usuário) podem.
ALTER TABLE CONTA ADD CONSTRAINT CK_CONTA_SALDO_USUARIO CHECK (USUARIO_ID IS NULL OR SALDO_CENTAVOS >= 0);

CREATE UNIQUE INDEX UK_CONTA_NUMERO ON CONTA (NUMERO);

-- Uma transferência só pode ter um estorno, mesmo com dois pedidos simultâneos.
CREATE UNIQUE INDEX UK_TRANSFERENCIA_ESTORNO ON TRANSFERENCIA (TRANSFERENCIA_ORIGINAL_ID)
    WHERE TRANSFERENCIA_ORIGINAL_ID IS NOT NULL;

-- Soma do limite diário: saídas de uma conta dentro do dia.
CREATE INDEX IDX_MOVIMENTO_CONTA_CRIADO_EM ON MOVIMENTO (CONTA_ID, CRIADO_EM);
