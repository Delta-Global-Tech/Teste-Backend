# Sistema de Contas e Transferências

Backend REST para gerenciamento de contas, depósitos e transferências financeiras, desenvolvido como solução para o teste técnico da Delta Global.

O projeto foi desenvolvido com foco em **consistência de saldo, concorrência, idempotência e rastreabilidade das movimentações financeiras**.

---

## Tecnologias

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Docker / Docker Compose
- Flyway
- JUnit 5
- Mockito

---
## Execução

### 1. Configuração de ambiente

O projeto utiliza variáveis de ambiente para configurações sensíveis e específicas do ambiente, como acesso ao PostgreSQL.

Crie um arquivo `.env` na raiz do projeto:

```
DB_PASSWORD=postgres
```

### 2. Subir o PostgreSQL

```bash
docker-compose up -d
```

O PostgreSQL é executado em container e as migrations do Flyway são aplicadas automaticamente quando a aplicação inicia.

### 3. Executar a aplicação

```bash
./mvnw spring-boot:run
```

A API fica disponível em:

```text
http://localhost:8080
```

### 4. Executar os testes

```bash
./mvnw test
```

---

# Arquitetura

O projeto utiliza uma separação entre:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

As regras de negócio ficam nos Services, enquanto os Controllers são responsáveis por receber requisições e retornar as respostas.

---

# Modelo de dados

As principais entidades utilizadas são:

- `conta`
- `transferencia`
- `movimento`
- `idempotencia`

Também existem contas especiais do sistema:

- `SISTEMA-ENTRADA`
- `SISTEMA-TAXAS`

A `SISTEMA-ENTRADA` representa a origem dos valores utilizados nos depósitos.
A `SISTEMA-TAXAS` recebe as taxas cobradas nas transferências.

Isso permite manter a movimentação financeira equilibrada: o dinheiro que sai de uma conta possui uma entrada correspondente em outra conta.

---

# Dinheiro em centavos

Os valores monetários são armazenados como inteiros em centavos utilizando `BIGINT` no PostgreSQL e `long`/`Long` na aplicação.

Exemplo:

```text
R$ 100,00 → 10000
R$ 50,00  → 5000
R$ 1,50   → 150
```

---

# Extrato imutável

Toda alteração de saldo gera um registro correspondente em `movimento`.

Um movimento possui, entre outros campos:

- conta
- transferência relacionada
- sequência
- tipo (`ENTRADA` ou `SAIDA`)
- valor em centavos
- saldo após o movimento
- data de criação

O valor do movimento é sempre positivo. O sentido financeiro é determinado pelo tipo.

## Imutabilidade

Movimentos não podem ser alterados ou removidos depois de gravados.

Essa regra não depende apenas da aplicação.

O PostgreSQL possui triggers que rejeitam diretamente operações de `UPDATE` e `DELETE` na tabela `movimento`.

Exemplo:

```sql
CREATE OR REPLACE FUNCTION impedir_alteracao_movimento()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Movimento é imutável';
END;
$$ LANGUAGE plpgsql;
```

---
# Concorrência

## Lock pessimista

Para operações que alteram saldo, as contas envolvidas são buscadas utilizando:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

Isso resulta em bloqueio da linha correspondente no PostgreSQL durante a transação.

### Constraint única

Constraints são utilizadas quando a regra é de unicidade.

Por exemplo, a idempotência possui uma restrição para uma primary key:

```text
endpoint + chave
```

A constraint não substitui o lock necessário para proteger o saldo, mas é fundamental para impedir que duas requisições criem a mesma chave de idempotência.

---

# Ordem dos locks

Quando uma transferência envolve três contas:

- origem;
- destino;
- conta de taxas;

os IDs são ordenados antes da aquisição dos locks.

```java
ids.sort(Long::compareTo);
```

Depois as contas são bloqueadas nessa mesma ordem.

Isso evita que uma transferência:

```text
A → B
```

adquira primeiro o lock de A enquanto outra transferência:

```text
B → A
```


Sem uma ordem global, as duas transações poderiam ficar esperando uma pela outra. Com a ordenação dos IDs, ambas tentam adquirir os locks na mesma ordem.

---

# Taxa de transferência

A regra implementada é:

- até R$ 100,00: sem taxa;
- acima de R$ 100,00: 1%;
- mínimo: R$ 1,00;
- máximo: R$ 20,00;
- arredondamento para o centavo mais próximo.

---

# Limite diário

O limite diário é calculado consultando as transferências armazenadas no banco.

O período do dia utiliza o fuso:

```text
America/Sao_Paulo
```

O valor utilizado no limite considera somente os valores transferidos pela conta de origem.

---

# Idempotência

Os endpoints que movimentam dinheiro utilizam `Idempotency-Key`.

A chave é persistida no PostgreSQL através da tabela `idempotencia`.

Não existe armazenamento da chave em memória como fonte da verdade.

A combinação:

```text
endpoint + chave
```

é única no banco.

A criação utiliza:

```sql
ON CONFLICT (endpoint, chave) DO NOTHING
```

Isso permite que somente uma requisição seja considerada responsável pela execução quando várias requisições chegam simultaneamente com a mesma chave.

## Mesmo corpo

Uma segunda requisição utilizando:

```text
mesmo endpoint
+
mesma chave
+
mesmo corpo
```

recupera a resposta anteriormente armazenada.

Nenhuma nova movimentação financeira é criada.

## Corpo diferente

Se a mesma chave for reutilizada com outro corpo, o hash da requisição é diferente.

Nesse caso, a operação é rejeitada com `409 Conflict`.

A requisição original não é executada novamente.

## Hash

O corpo da requisição é convertido para SHA-256.

A tabela de idempotência armazena:

- endpoint;
- chave;
- hash da requisição;
- resposta JSON;
- status HTTP;
- data de criação.

A resposta original é armazenada para que o retry possa reproduzir o resultado da primeira operação.

---
# Estados da conta

As contas possuem os estados:

```text
ATIVA
BLOQUEADA
ENCERRADA
```

As regras são aplicadas na camada de negócio.
Uma conta bloqueada não pode realizar transferências de saída, mas pode receber valores.
Uma conta encerrada não pode realizar novas movimentações.

---
# Migrations

O banco é criado e versionado utilizando Flyway.
As migrations são executadas automaticamente durante a inicialização da aplicação.
Entre as responsabilidades das migrations estão:

- criação das tabelas;
- criação das constraints;
- criação dos índices;
- criação das triggers de imutabilidade;
- criação das contas do sistema;
- carga inicial;
- registros necessários para o funcionamento inicial do sistema.

---
# Testes

## Testes unitários

Os testes unitários utilizam:
- JUnit 5;
- Mockito.

Atualmente existem testes para regras como:
- cálculo da taxa;
- taxa mínima;
- taxa máxima;
- cálculo determinístico do hash de idempotência;
- regras do serviço de transferência;
- saldo insuficiente;
- limite diário;
- reutilização de chave de idempotência;
- conflito de chave com corpo diferente.

Exemplo:

```bash
./mvnw test
```

---

# Validação manual

Os principais fluxos também foram validados através do Bruno.

## Depósito

Exemplo:

```http
POST /contas/CONTA-001/depositos
```

```json
{
   "valorCentavos": 10000
}
```

com:

```text
Idempotency-Key: deposito-real-001
```

Foi validado que o depósito altera o saldo e cria o movimento correspondente.
O reenvio da mesma requisição não cria um segundo movimento.
A reutilização da chave com outro valor é rejeitada.

## Transferência

Também foi validado o fluxo entre:

```text
CONTA-001
      ↓
CONTA-002
```

A validação considera:
- alteração dos saldos;
- criação dos movimentos;
- idempotência;
- conflito de chave.


---
# O que foi priorizado

O desenvolvimento priorizou inicialmente as partes que afetam diretamente a consistência financeira:

1. modelagem do banco;
2. migrations;
3. contas e saldos;
4. movimentos;
5. imutabilidade no banco;
6. transferências;
7. cálculo de taxas;
8. limite diário;
9. concorrência através de locks no banco;
10. idempotência;
11. testes unitários;
12. testes de integração.

---
# O que ficou de fora

Algumas funcionalidades do desafio ainda não foram implementadas nesta versão:

- estorno;
- transferências agendadas;
- job de execução dos agendamentos;
- endpoint de resumo;
- endpoint `DELETE /dados`;
- concorrência completa com threads para todos os cenários exigidos;
- Testcontainers para os testes de integração;
- documentação OpenAPI;
- hash encadeado dos movimentos;
- métricas de conflitos de concorrência;
- teste de carga.

---