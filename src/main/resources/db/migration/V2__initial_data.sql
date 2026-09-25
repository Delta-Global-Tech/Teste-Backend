
-- ---------------------------------------------------------------
-- 1. Contas do sistema (sem usuário)
-- ---------------------------------------------------------------
INSERT INTO conta (numero, usuario_id, saldo_centavos, limite_diario_centavos, estado, versao)
VALUES
    ('SISTEMA-ENTRADA', NULL, 0, 999999999999, 'ATIVA', 0),
    ('SISTEMA-TAXAS',   NULL, 0, 999999999999, 'ATIVA', 0);

-- ---------------------------------------------------------------
-- 2. Usuários
-- ---------------------------------------------------------------
INSERT INTO usuario (nome, cpf) VALUES
    ('Alice Souza',    '11122233344'),
    ('Bruno Lima',     '22233344455'),
    ('Carla Pereira',  '33344455566'),
    ('Daniel Costa',   '44455566677'),
    ('Elena Martins',  '55566677788');

-- Usuário extra para contas especiais (bloqueada/encerrada)
INSERT INTO usuario (nome, cpf) VALUES
    ('Fernando Alves', '66677788899'),
    ('Gabriela Rocha', '77788899900');

-- ---------------------------------------------------------------
-- 3. Contas dos usuários
-- ---------------------------------------------------------------
-- 5 contas ATIVA com R$ 1.000,00 cada (100000 centavos) e limite R$ 2.000,00
INSERT INTO conta (numero, usuario_id, saldo_centavos, limite_diario_centavos, estado, versao)
VALUES
    ('CONTA-001', (SELECT id FROM usuario WHERE cpf = '11122233344'), 100000, 200000, 'ATIVA',     0),
    ('CONTA-002', (SELECT id FROM usuario WHERE cpf = '22233344455'), 100000, 200000, 'ATIVA',     0),
    ('CONTA-003', (SELECT id FROM usuario WHERE cpf = '33344455566'), 100000, 200000, 'ATIVA',     0),
    ('CONTA-004', (SELECT id FROM usuario WHERE cpf = '44455566677'), 100000, 200000, 'ATIVA',     0),
    ('CONTA-005', (SELECT id FROM usuario WHERE cpf = '55566677788'), 100000, 200000, 'ATIVA',     0),
    ('CONTA-006', (SELECT id FROM usuario WHERE cpf = '66677788899'),  30000, 200000, 'BLOQUEADA', 0),
    ('CONTA-007', (SELECT id FROM usuario WHERE cpf = '77788899900'),      0, 200000, 'ENCERRADA', 0);


-- Transferências de carga para as 5 contas ATIVA (R$ 1.000,00 = 100000 centavos)
INSERT INTO transferencia (conta_origem_id, conta_destino_id, valor_centavos, taxa_centavos, estado, concluida_em)
SELECT
    (SELECT id FROM conta WHERE numero = 'SISTEMA-ENTRADA'),
    c.id,
    100000,
    0,
    'CONFIRMADA',
    NOW()
FROM conta c
WHERE c.numero IN ('CONTA-001', 'CONTA-002', 'CONTA-003', 'CONTA-004', 'CONTA-005');

-- Transferência de carga para CONTA-006 (BLOQUEADA, R$ 300,00 = 30000 centavos)
INSERT INTO transferencia (conta_origem_id, conta_destino_id, valor_centavos, taxa_centavos, estado, concluida_em)
SELECT
    (SELECT id FROM conta WHERE numero = 'SISTEMA-ENTRADA'),
    c.id,
    30000,
    0,
    'CONFIRMADA',
    NOW()
FROM conta c
WHERE c.numero = 'CONTA-006';

-- ---------------------------------------------------------------
-- 5. Movimentos de SAIDA na SISTEMA-ENTRADA
--    (sequência 1..6 para a SISTEMA-ENTRADA)
-- ---------------------------------------------------------------
DO $$
DECLARE
    v_sistema_entrada_id BIGINT;
    v_conta             RECORD;
    v_seq               BIGINT := 1;
    v_saldo_sistema     BIGINT := 0;
    v_transferencia_id  BIGINT;
BEGIN
    SELECT id INTO v_sistema_entrada_id FROM conta WHERE numero = 'SISTEMA-ENTRADA';

    -- Para cada conta que recebeu carga, na ordem de criação
    FOR v_conta IN
        SELECT c.id AS conta_id, c.numero, t.valor_centavos, t.id AS transferencia_id
        FROM conta c
        JOIN transferencia t ON t.conta_destino_id = c.id
                             AND t.conta_origem_id = v_sistema_entrada_id
                             AND t.estado = 'CONFIRMADA'
        ORDER BY c.id
    LOOP
        -- Saldo da SISTEMA-ENTRADA vai ficando negativo
        v_saldo_sistema := v_saldo_sistema - v_conta.valor_centavos;

        INSERT INTO movimento (conta_id, transferencia_id, sequencia, tipo, valor_centavos, saldo_apos_centavos)
        VALUES (
            v_sistema_entrada_id,
            v_conta.transferencia_id,
            v_seq,
            'SAIDA',
            v_conta.valor_centavos,
            v_saldo_sistema
        );

        v_seq := v_seq + 1;
    END LOOP;

    UPDATE conta SET saldo_centavos = v_saldo_sistema WHERE id = v_sistema_entrada_id;
END $$;

-- ---------------------------------------------------------------
-- 6. Movimentos de ENTRADA nas contas dos usuários (sequência 1)
-- ---------------------------------------------------------------
DO $$
DECLARE
    v_sistema_entrada_id BIGINT;
    v_conta              RECORD;
BEGIN
    SELECT id INTO v_sistema_entrada_id FROM conta WHERE numero = 'SISTEMA-ENTRADA';

    FOR v_conta IN
        SELECT c.id AS conta_id, c.numero, t.valor_centavos, t.id AS transferencia_id
        FROM conta c
        JOIN transferencia t ON t.conta_destino_id = c.id
                             AND t.conta_origem_id = v_sistema_entrada_id
                             AND t.estado = 'CONFIRMADA'
        ORDER BY c.id
    LOOP
        INSERT INTO movimento (conta_id, transferencia_id, sequencia, tipo, valor_centavos, saldo_apos_centavos)
        VALUES (
            v_conta.conta_id,
            v_conta.transferencia_id,
            1,
            'ENTRADA',
            v_conta.valor_centavos,
            v_conta.valor_centavos  
        );
    END LOOP;
END $$;

-- ---------------------------------------------------------------
-- 7. Verificação de consistência pós-carga
--    (garante que a soma de todos os movimentos é zero)
-- ---------------------------------------------------------------
DO $$
DECLARE
    v_soma BIGINT;
BEGIN
    SELECT COALESCE(SUM(
        CASE tipo
            WHEN 'ENTRADA' THEN  valor_centavos
            WHEN 'SAIDA'   THEN -valor_centavos
        END
    ), 0)
    INTO v_soma
    FROM movimento;

    IF v_soma <> 0 THEN
        RAISE EXCEPTION 'INCONSISTÊNCIA: soma de movimentos = % (esperado 0)', v_soma;
    END IF;

    RAISE NOTICE 'Carga inicial OK: soma dos movimentos = 0';
END $$;
