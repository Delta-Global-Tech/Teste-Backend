INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    1,
    NULL,
    1,
    'SAIDA',
    100000,
    -100000
);
UPDATE conta
SET saldo_centavos = -100000
WHERE id = 1;

INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    3,
    NULL,
    1,
    'ENTRADA',
    100000,
    100000
);
UPDATE conta
SET saldo_centavos = 100000
WHERE id= 3;


INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    1,
    NULL,
    2,
    'SAIDA',
    100000,
    -200000
);
UPDATE conta
SET saldo_centavos = -200000
WHERE id = 1;

INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    4,
    NULL,
    1,
    'ENTRADA',
    100000,
    100000
);
UPDATE conta
SET saldo_centavos = 100000
WHERE id= 4;


INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    1,
    NULL,
    3,
    'SAIDA',
    100000,
    -300000
);
UPDATE conta
SET saldo_centavos = -300000
WHERE id = 1;

INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    5,
    NULL,
    1,
    'ENTRADA',
    100000,
    100000
);
UPDATE conta
SET saldo_centavos = 100000
WHERE id= 5;


INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    1,
    NULL,
    4,
    'SAIDA',
    100000,
    -400000
);
UPDATE conta
SET saldo_centavos = -400000
WHERE id = 1;

INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    6,
    NULL,
    1,
    'ENTRADA',
    100000,
    100000
);
UPDATE conta
SET saldo_centavos = 100000
WHERE id= 6;


INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    1,
    NULL,
    5,
    'SAIDA',
    100000,
    -500000
);
UPDATE conta
SET saldo_centavos = -500000
WHERE id = 1;

INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    7,
    NULL,
    1,
    'ENTRADA',
    100000,
    100000
);
UPDATE conta
SET saldo_centavos = 100000
WHERE id= 7;


INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    1,
    NULL,
    6,
    'SAIDA',
    30000,
    -530000
);
UPDATE conta
SET saldo_centavos = -530000
WHERE id = 1;

INSERT INTO movimento (
    conta_id,
    transferencia_id,
    sequencia,
    tipo,
    valor_centavos,
    saldo_apos_centavos
)
VALUES (
    8,
    NULL,
    1,
    'ENTRADA',
    30000,
    30000
);
UPDATE conta
SET saldo_centavos = 30000
WHERE id= 8;