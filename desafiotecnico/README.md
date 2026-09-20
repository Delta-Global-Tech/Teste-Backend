# Desafio Técnico — Sistema de Contas e Transferências

Backend desenvolvido em Java com Spring Boot para gerenciamento de contas, depósitos, transferências, estornos, extratos e idempotência.

O principal objetivo do projeto é garantir que o saldo permaneça consistente mesmo com múltiplas requisições simultâneas e duas instâncias da aplicação acessando o mesmo banco de dados.

---

## Tecnologias utilizadas

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway
- JUnit 5
- Mockito
- Testcontainers
- Docker e Docker Compose

---

## Como executar o projeto

### Pré-requisitos

- Java 21
- Maven
- Docker
- Docker Compose

### Subir o PostgreSQL

```bash
docker compose up -d postgres
```

### Executar a aplicação

```bash
./mvnw spring-boot:run
```

Ou, usando Maven instalado:

```bash
mvn spring-boot:run
```

### Executar todos os testes

```bash
mvn clean test
```

### Executar apenas os testes de integração

```bash
mvn -Dtest="*IntegrationTest" test
```

---

## Banco de dados

O banco utilizado é o PostgreSQL.

As alterações de estrutura e carga inicial devem ser controladas pelo Flyway. A aplicação não utiliza `ddl-auto=update` nem `ddl-auto=create`.

Configuração esperada:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

As migrations ficam em:

```text
src/main/resources/db/migration
```

---

## Modelo de dados

O sistema possui as seguintes tabelas principais:

- `users`
- `accounts`
- `transfers`
- `movements`
- `scheduled`
- `idempotency_records`

### Contas do sistema

Existem duas contas especiais:

- `SISTEMA-ENTRADA`
- `SISTEMA-TAXAS`

A conta `SISTEMA-ENTRADA` representa a origem dos depósitos.

A conta `SISTEMA-TAXAS` recebe as taxas cobradas nas transferências.

Essas contas não possuem usuário associado.

---

## Valores monetários

Os valores financeiros são persistidos em centavos utilizando `BIGINT`.

Exemplo:

```text
R$ 100,00 = 10.000 centavos
```

A taxa é calculada com `BigDecimal` e `RoundingMode.HALF_UP`.

A regra implementada é:

- transferências até R$ 100,00: sem taxa;
- acima de R$ 100,00: 1%;
- taxa mínima: R$ 1,00;
- taxa máxima: R$ 20,00.

Exemplos:

| Valor | Taxa |
|---:|---:|
| R$ 10,00 | R$ 0,00 |
| R$ 100,00 | R$ 0,00 |
| R$ 100,01 | R$ 1,00 |
| R$ 112,50 | R$ 1,13 |
| R$ 150,00 | R$ 1,50 |
| R$ 1.234,56 | R$ 12,35 |
| R$ 2.000,00 | R$ 20,00 |
| R$ 5.000,00 | R$ 20,00 |

---

# Concorrência e consistência

## Controle do saldo

A atualização das contas utiliza transação e lock pessimista no banco:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Optional<Account> findByIdForUpdate(Long id);
```

A escolha pelo `SELECT ... FOR UPDATE` foi feita porque a operação de transferência precisa:

1. ler o saldo atual;
2. validar saldo suficiente;
3. validar limite diário;
4. atualizar a conta de origem;
5. atualizar a conta de destino;
6. registrar os movimentos.

Todas essas etapas precisam ocorrer dentro da mesma transação.

### Alternativas consideradas

#### `@Version`

O lock otimista com `@Version` detecta conflitos somente no momento do `UPDATE`.

Vantagens:

- menor bloqueio no banco;
- bom para cenários com poucos conflitos.

Desvantagens:

- uma das transações falha com `OptimisticLockException`;
- seria necessário implementar retry;
- a validação de saldo e limite poderia ser feita com dados que ficaram desatualizados;
- aumenta a complexidade da operação.

O campo `version` permanece como proteção adicional, mas o controle principal da transferência é feito com lock pessimista.

#### `synchronized`

Não foi utilizado como mecanismo de consistência porque protege apenas uma instância da JVM.

Com duas instâncias da API, cada processo teria seu próprio lock, permitindo que duas transações alterassem o mesmo saldo simultaneamente.

#### Constraint única

Constraints únicas são utilizadas para garantir regras estruturais, como:

- número da conta;
- CPF;
- sequência do movimento por conta;
- chave de idempotência por endpoint;
- apenas um estorno por transferência original.

A constraint não substitui o lock para validação de saldo e limite.

---

## Ordem global dos locks

Transferências cruzadas podem gerar deadlock:

```text
CONTA-001 -> CONTA-002
CONTA-002 -> CONTA-001
```

Se cada transação bloquear primeiro a sua conta de origem, o banco pode chegar à seguinte situação:

```text
Transação A bloqueia CONTA-001 e espera CONTA-002
Transação B bloqueia CONTA-002 e espera CONTA-001
```

Para evitar isso, as contas são bloqueadas sempre pela ordem crescente dos seus IDs:

```java
ids.stream()
        .distinct()
        .sorted()
        .map(id -> accountRepository.findByIdForUpdate(id))
```

Assim, mesmo que a transferência seja invertida, as duas transações tentam adquirir os locks na mesma ordem.

Com isso, uma transação pode aguardar a outra, mas não existe espera circular.

Esse comportamento é validado pelo teste de transferências cruzadas simultâneas.

---

## Limite diário

O limite diário é calculado consultando as transferências confirmadas no banco:

```sql
SELECT COALESCE(SUM(amount_cents), 0)
FROM transfers
WHERE source_account_id = ?
  AND status = 'CONFIRMED'
  AND created_at >= ?
  AND created_at < ?
```

O intervalo de datas é calculado utilizando o fuso:

```text
America/Sao_Paulo
```

As taxas não entram no limite diário. Apenas o valor principal da transferência é contabilizado.

Exemplo:

```text
Limite: R$ 1.000,00
Transferências anteriores: R$ 900,00
Nova transferência: R$ 100,01
```

Resultado:

```text
Limite excedido
```

A taxa da nova transferência não é considerada nessa soma.

### Concorrência do limite diário

A conta de origem é bloqueada antes da leitura do total utilizado no dia.

Dessa forma:

1. a primeira transação bloqueia a conta;
2. consulta o valor utilizado;
3. valida o limite;
4. grava a transferência;
5. atualiza o saldo;
6. libera o lock;
7. a segunda transação consulta o novo estado.

Assim, quando duas transferências simultâneas ultrapassariam o limite em conjunto, somente uma é confirmada.

---

# Extrato imutável

Cada alteração de saldo deve gerar movimentos.

Um movimento contém:

- conta;
- transferência responsável;
- sequência;
- tipo;
- valor;
- saldo após o movimento;
- data de criação.

A sequência é controlada por conta e possui constraint única:

```sql
CONSTRAINT uk_movements_account_sequence
UNIQUE (account_id, sequence_number)
```

A próxima sequência é obtida consultando o último movimento da conta.

A conta de origem recebe um movimento de saída e a conta de destino recebe um movimento de entrada.

Quando há taxa:

- a origem recebe um movimento de saída referente ao valor transferido;
- a origem recebe outro movimento de saída referente à taxa;
- a conta `SISTEMA-TAXAS` recebe um movimento de entrada.

---

## Imutabilidade no banco

A imutabilidade não deve depender apenas do código Java.

A proteção deve ser criada diretamente no PostgreSQL por meio de uma trigger.

Migration recomendada:

```sql
CREATE OR REPLACE FUNCTION prevent_movement_changes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Movements are immutable and cannot be updated or deleted';

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_movements_immutable
BEFORE UPDATE OR DELETE
ON movements
FOR EACH ROW
EXECUTE FUNCTION prevent_movement_changes();
```

Essa proteção impede:

```sql
UPDATE movements ...
```

e:

```sql
DELETE FROM movements ...
```

mesmo que a alteração seja executada diretamente no banco.

O teste de integração correspondente executa as duas operações e espera uma exceção do PostgreSQL.

---

# Idempotência

As chaves de idempotência são persistidas na tabela:

```text
idempotency_records
```

Existe uma constraint única:

```sql
UNIQUE (endpoint, idempotency_key)
```

A chave é válida por endpoint. Assim, a mesma chave pode existir em endpoints diferentes, mas não pode ser repetida no mesmo endpoint.

Quando uma requisição é recebida:

1. o sistema procura a chave para o endpoint;
2. se não existir, executa a operação;
3. salva a resposta e o status HTTP;
4. se existir com o mesmo corpo, devolve a resposta original;
5. se existir com corpo diferente, retorna `409`.

A gravação da chave deve ocorrer na mesma transação da movimentação financeira.

## Concorrência da idempotência

Em requisições simultâneas com a mesma chave, a constraint única do banco impede a criação de dois registros para a mesma combinação:

```text
endpoint + idempotency_key
```

O tratamento da violação da constraint deve retornar a resposta já persistida ou repetir a leitura do registro vencedor.

Esse cenário é validado por teste com múltiplas threads utilizando a mesma chave.

---

# Estorno

O estorno cria uma nova transferência no sentido inverso.

A transferência original:

- não tem seu valor alterado;
- não tem seus movimentos apagados;
- muda apenas o estado para `REVERSED`.

A nova transferência aponta para a original por meio de:

```text
original_transfer_id
```

Existe uma constraint única:

```sql
UNIQUE (original_transfer_id)
```

Essa constraint garante que uma transferência não possua dois estornos.

Para evitar dois estornos simultâneos:

1. as contas envolvidas são bloqueadas na mesma ordem global;
2. a transferência original precisa estar confirmada;
3. a nova transferência é criada;
4. a original é marcada como estornada;
5. os movimentos do estorno são gravados.

A decisão adotada para estorno sem saldo é:

```text
O estorno é recusado com HTTP 422.
```

Motivo: não permitir que uma conta de usuário fique negativa e não criar dinheiro artificialmente no sistema.

---

# Estados

As transições de estado são controladas na entidade.

Transferências podem seguir:

```text
CREATED -> CONFIRMED
CREATED -> FAILED
CONFIRMED -> REVERSED
```

Transições fora dessas regras devem gerar uma exceção de estado inválido.

A transferência original não é substituída pelo estorno. O estorno é uma nova transferência.

---

# Endpoints principais

## Depósito

```http
POST /contas/{numero}/depositos
```

Header obrigatório:

```http
Idempotency-Key: chave-unica
```

Exemplo:

```json
{
  "valor": 250.00
}
```

## Transferência

```http
POST /transferencias
```

Header obrigatório:

```http
Idempotency-Key: chave-unica
```

Exemplo:

```json
{
  "contaOrigem": "CONTA-001",
  "contaDestino": "CONTA-002",
  "valor": 150.00
}
```

## Estorno

```http
POST /transferencias/{id}/estorno
```

Header obrigatório:

```http
Idempotency-Key: chave-unica
```

## Extrato

```http
GET /contas/{numero}/extrato
```

Parâmetros opcionais:

```text
de
ate
page
size
```

## Resumo

```http
GET /resumo
```

Parâmetro opcional:

```text
janelaMinutos
```

## Reset de dados

```http
DELETE /dados
```

Disponível somente para ambiente de desenvolvimento e testes.

---

# Códigos de erro

Os erros devem retornar um corpo padronizado contendo:

```json
{
  "timestamp": "2026-09-10T22:05:00.000-03:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "codigo": "SALDO_INSUFICIENTE",
  "message": "Saldo insuficiente para transferir",
  "path": "/transferencias"
}
```

Principais códigos:

| HTTP | Código | Situação |
|---:|---|---|
| 400 | `REQUISICAO_INVALIDA` | Corpo inválido ou header ausente |
| 404 | `RECURSO_NAO_ENCONTRADO` | Conta ou transferência inexistente |
| 409 | `CONTA_NAO_PODE_ENVIAR` | Conta bloqueada ou encerrada tentando enviar |
| 409 | `CONTA_ENCERRADA` | Conta encerrada tentando receber |
| 409 | `IDEMPOTENCY_KEY_CONFLITANTE` | Mesma chave com corpo diferente |
| 409 | `TRANSFERENCIA_NAO_CONFIRMADA` | Estorno de transferência inválida |
| 422 | `VALOR_INVALIDO` | Valor menor ou igual a zero |
| 422 | `SALDO_INSUFICIENTE` | Saldo insuficiente |
| 422 | `LIMITE_DIARIO_EXCEDIDO` | Limite diário ultrapassado |

---

# Índices

Os seguintes índices são utilizados:

```sql
CREATE INDEX idx_transfers_created_at
ON transfers (created_at);
```

Utilizado no resumo de transferências por período.

```sql
CREATE INDEX idx_transfers_status_created_at
ON transfers (status, created_at);
```

Utilizado no cálculo de transferências confirmadas dentro do limite diário e nas consultas por estado e período.

```sql
CREATE INDEX idx_movements_account_sequence
ON movements (account_id, sequence_number DESC);
```

Utilizado para buscar o último movimento da conta e montar o extrato.

```sql
CREATE INDEX idx_movements_account_created_at
ON movements (account_id, created_at);
```

Utilizado para consultas do extrato por conta e período.

```sql
CREATE INDEX idx_movements_transfer
ON movements (transfer_id);
```

Utilizado para localizar os movimentos associados a uma transferência.

```sql
CREATE INDEX idx_scheduled_due
ON scheduled (state, execute_at);
```

Utilizado para buscar agendamentos vencidos e ainda pendentes.

---

# Testes

## Testes unitários

Os testes unitários utilizam JUnit 5 e Mockito.

São cobertos:

- cálculo de taxa;
- transferência dentro do limite;
- limite diário excedido;
- taxa fora do cálculo do limite;
- saldo insuficiente;
- conta bloqueada;
- conta encerrada;
- transições inválidas de estado.

## Testes de integração

Os testes de integração utilizam Testcontainers com PostgreSQL real.

São cobertos:

- subida do PostgreSQL;
- execução das migrations;
- carga inicial;
- depósitos;
- transferências;
- estornos;
- contas inexistentes;
- contas bloqueadas;
- contas encerradas;
- saldo insuficiente;
- limite diário excedido;
- idempotência;
- imutabilidade dos movimentos.

## Testes de concorrência

Os testes utilizam múltiplas threads e `CountDownLatch` para iniciar as operações simultaneamente.

Cenários previstos:

- duas transferências da mesma conta com saldo para apenas uma;
- duas transferências que juntas ultrapassam o limite diário;
- transferências cruzadas;
- várias requisições com a mesma chave de idempotência;
- dois estornos simultâneos.

Os testes validam:

- quantidade de operações confirmadas;
- saldo final;
- quantidade de transferências;
- quantidade de movimentos;
- soma global dos movimentos.

---

# Consistência financeira

A consistência é validada por três regras.

## Saldo da conta

O saldo armazenado em `accounts.balance_cents` deve ser igual ao saldo do último movimento da conta.

## Soma dos movimentos da conta

Para cada conta:

```text
saldo = soma das entradas - soma das saídas
```

## Soma global

Para todas as contas do sistema:

```text
soma das entradas - soma das saídas = 0
```

Essa validação inclui as contas:

- de usuários;
- `SISTEMA-ENTRADA`;
- `SISTEMA-TAXAS`.

---

# Agendamentos

A tabela de agendamentos existe e possui índice por:

```text
state + execute_at
```

A execução automática dos agendamentos precisa garantir:

- apenas uma instância processe cada agendamento;
- duas instâncias não executem o mesmo agendamento;
- agendamentos vencidos sejam localizados no banco;
- falhas sejam registradas;
- o agendamento não fique travado caso uma instância caia.

A estratégia escolhida é utilizar seleção com lock no banco:

```sql
SELECT *
FROM scheduled
WHERE state = 'SCHEDULED'
  AND execute_at <= now()
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

O registro deve ser alterado para um estado de processamento dentro da mesma transação antes da execução da transferência.

A coluna `failure_reason` registra o motivo quando o agendamento falha por saldo insuficiente, limite excedido ou outra regra de negócio.

---

# Decisões em aberto

## Estorno sem saldo

Foi escolhido rejeitar o estorno com `422` quando a conta que recebeu a transferência não possui saldo suficiente.

Essa decisão evita:

- saldo negativo em conta de usuário;
- criação de dinheiro sem origem;
- inconsistência entre conta e movimentos.

Uma alternativa seria deixar a conta negativa, mas isso violaria a regra de que contas de usuário não podem ficar negativas.

Outra alternativa seria criar um estorno pendente, porém isso exigiria um novo fluxo de liquidação e acompanhamento.

## Limite diário após estorno

A decisão adotada é não devolver automaticamente o limite diário consumido pelo valor original.

O estorno é uma nova transferência no sentido oposto e não deve ser utilizado para contornar o limite diário.

Essa escolha evita que um usuário:

1. faça uma transferência;
2. consuma o limite;
3. estorne;
4. reutilize o limite novamente de forma artificial.

---

# Conclusão

A solução utiliza o banco de dados como fonte da verdade para:

- saldos;
- transferências;
- movimentos;
- idempotência;
- estados;
- agendamentos.

A proteção contra concorrência não depende de variáveis em memória, `synchronized` ou locks da JVM.

O controle principal é feito com:

- transações;
- `SELECT ... FOR UPDATE`;
- ordem global de locks;
- constraints únicas;
- migrations;
- testes concorrentes com PostgreSQL real.

O foco principal do projeto é manter a consistência financeira mesmo quando múltiplas requisições são executadas simultaneamente.