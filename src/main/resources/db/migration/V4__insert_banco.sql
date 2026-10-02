-- sistema
INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES (1,'SISTEMA-ENTRADA',NULL,0,0,'ATIVA',1);
INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES ( 2,'SISTEMA-TAXAS',NULL,0,0,'ATIVA',1);

-- usuarios
INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES (3,'CONTA-001',1,0,200000,'ATIVA',1);

INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES (4,'CONTA-002',2,0,200000,'ATIVA',1);

INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES ( 5, 'CONTA-003', 3, 0, 200000, 'ATIVA',1);

INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES (6,'CONTA-004',4,0,200000,'ATIVA',1);

INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES ( 7,'CONTA-005',5,0,200000,'ATIVA',1 );

INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES (8,'CONTA-006', 6, 0, 0, 'BLOQUEADA', 1);

INSERT INTO conta (
    id,
    numero,
    usuario_id,
    saldo_centavos,
    limite_diario_centavos,
    estado,
    versao
)
OVERRIDING SYSTEM VALUE
VALUES (9,'CONTA-007',7,0,0,'ENCERRADA',1);