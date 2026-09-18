-- Contas de sistema
INSERT INTO accounts (account_number, balance_cents, daily_limit_cents, account_status, user_id, version)
VALUES('SISTEMA-ENTRADA', 0, 0, 'ACTIVE', NULL, 0),
      ('SISTEMA-TAXAS', 0, 0, 'ACTIVE', NULL, 0);

-- Usuários
INSERT INTO users (name, cpf) VALUES('Usuario Um', '11111111111'),('Usuario Dois', '22222222222'),('Usuario Tres', '33333333333'),('Usuario Quatro', '44444444444'),('Usuario Cinco', '55555555555');

-- Contas ativas dos 5 usuários, saldo R$ 1.000,00, limite diário R$ 2.000,00
INSERT INTO accounts (account_number, balance_cents, daily_limit_cents, account_status, user_id, version)
SELECT 'CONTA-00' || u.id, 100000, 200000, 'ACTIVE', u.id, 0
FROM users u;

-- Conta bloqueada, saldo R$ 300,00
INSERT INTO accounts (account_number, balance_cents, daily_limit_cents, account_status, user_id, version)
VALUES ('CONTA-BLOQUEADA', 30000, 200000, 'BLOCKED', NULL, 0);

-- Conta encerrada, saldo zero
INSERT INTO accounts (account_number, balance_cents, daily_limit_cents, account_status, user_id, version)
VALUES ('CONTA-ENCERRADA', 0, 200000, 'CLOSED', NULL, 0);

-- Transferência "sistema" representando a carga inicial de cada conta com saldo > 0
INSERT INTO transfers (source_account_id, destination_account_id, amount_cents, fee_cents, status, created_at, completed_at)
SELECT
    (SELECT id FROM accounts WHERE account_number = 'SISTEMA-ENTRADA'),
    a.id,
    a.balance_cents,
    0,
    'CONFIRMED',
    now(),
    now()
FROM accounts a
WHERE a.account_number NOT IN ('SISTEMA-ENTRADA', 'SISTEMA-TAXAS')
  AND a.balance_cents > 0;

-- Movimentos de SAIDA na SISTEMA-ENTRADA para cada carga inicial
INSERT INTO movements (account_id, transfer_id, sequence_number, type, amount_cents, balance_after_cents, created_at)
SELECT
    (SELECT id FROM accounts WHERE account_number = 'SISTEMA-ENTRADA'),
    t.id,
    ROW_NUMBER() OVER (ORDER BY t.id),
    'DEBIT',
    t.amount_cents,
    -1 * SUM(t.amount_cents) OVER (ORDER BY t.id),
    t.created_at
FROM transfers t
WHERE t.source_account_id = (SELECT id FROM accounts WHERE account_number = 'SISTEMA-ENTRADA');

-- Movimentos de ENTRADA em cada conta que recebeu carga inicial
INSERT INTO movements (account_id, transfer_id, sequence_number, type, amount_cents, balance_after_cents, created_at)
SELECT
    t.destination_account_id,
    t.id,
    1,
    'CREDIT',
    t.amount_cents,
    t.amount_cents,
    t.created_at
FROM transfers t
WHERE t.source_account_id = (SELECT id FROM accounts WHERE account_number = 'SISTEMA-ENTRADA');

-- Atualiza o saldo da SISTEMA-ENTRADA para negativo, refletindo o total distribuído
UPDATE accounts
SET balance_cents = -1 * (
    SELECT COALESCE(SUM(amount_cents), 0)
    FROM transfers
    WHERE source_account_id = accounts.id
)
WHERE account_number = 'SISTEMA-ENTRADA';