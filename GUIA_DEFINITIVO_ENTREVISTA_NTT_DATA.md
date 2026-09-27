# 🎓 GUIA DEFINITIVO: ENTREVISTA TÉCNICA JAVA PLENO — NTT DATA
**Candidato:** Matheus Araujo Pereira (MAP)  
**Projeto de Referência:** MAP-Bank (`com.mapbank`)  
**Temas Centrais Solicitados pelo Avaliador:** Java 17 e 21, Spring Boot 3, Observabilidade, SQL/PostgreSQL e Boas Práticas Bancárias  

---

## 🧭 SUMÁRIO EXECUTIVO

Este guia foi elaborado para ser o seu **arsenal técnico definitivo**. Ele cobre com profundidade conceitual e exemplos de código cada um dos pontos que o entrevistador da NTT DATA pontuou que seriam cobrados na etapa técnica.

---

# ☕ PARTE 1: JAVA 17 E JAVA 21 EM PROFUNDIDADE

### 1.1. Virtual Threads (Project Loom) — JEP 444 (Java 21)
- **O que são?**  
  Threads gerenciadas pela própria JVM em memória heap, desacopladas 1:1 das threads pesadas do Sistema Operacional (*Platform Threads*).
- **Por que é uma revolução para sistemas bancários?**  
  Em modelos tradicionais (*thread-per-request* com Platform Threads), criar 5.000 threads consome ~5 GB de memória de pilha do S.O. (1 MB por thread) e satura o escalonador do Linux com trocas de contexto (*context switching*). Com **Virtual Threads**, cada thread consome apenas ~1 KB no heap. É possível ter **1.000.000 de Virtual Threads** simultâneas.
- **Como funcionam internamente (Carrier Threads & ForkJoinPool)?**  
  A JVM mantém um pool interno de Platform Threads chamado *Carrier Threads* (geralmente igual ao número de núcleos da CPU). Quando uma Virtual Thread executa uma operação bloqueante de I/O (ex.: consulta SQL no PostgreSQL via JDBC, chamada HTTP de PIX via RestTemplate/WebClient), a JVM **desmonta (unmounts)** a Virtual Thread da Carrier Thread e a suspende. A Carrier Thread fica imediatamente livre para processar outra requisição. Quando o I/O responde, a Virtual Thread é remontada em qualquer Carrier Thread disponível.
- **O que é Thread Pinning?**  
  Ocorre quando uma Virtual Thread não consegue ser desmontada da Carrier Thread durante um bloqueio de I/O. Isso acontecia no Java 21 dentro de blocos `synchronized` ou chamadas nativas JNI.  
  *Dica de Ouro para a Entrevista:* Explique que a boa prática recomendada para evitar pinning é substituir `synchronized` por `java.util.concurrent.locks.ReentrantLock`. No Java 24 esse comportamento foi refinado, mas demonstrar o conhecimento de `ReentrantLock` pontua alto.
- **Como ativamos no Spring Boot 3?**  
  Basta uma única linha no `application.yml`:
  ```yaml
  spring:
    threads:
      virtual:
        enabled: true
  ```
  O Spring Boot configura automaticamente o Tomcat embutido e os executores assíncronos (`@Async`) para despacharem cada requisição HTTP em uma nova Virtual Thread.

---

### 1.2. Java Records (Java 14/16/17)
- **O que são?**  
  Classes imutáveis transparentes introduzidas para funcionar como portadores de dados (*data carriers*).
- **O que o compilador gera automaticamente?**  
  - Construtor canônico com todos os atributos.
  - Métodos acessores (sem prefixo `get`: ex.: `request.amount()` e não `request.getAmount()`).
  - `equals()` e `hashCode()` baseados no estado dos campos.
  - `toString()` formatado.
  - Todos os campos são implicitamente `private final` e a classe é `final`.
- **Por que usamos Records nos DTOs do MAP-Bank?**  
  - **Thread-safety:** Por serem imutáveis, instâncias de Records podem ser compartilhadas com segurança entre múltiplas threads sem risco de mutação de estado.
  - **Adequação ao Domínio Bancário:** Um payload de transferência recebido na API não deve ter seu valor ou destinatário alterado após ser desserializado.
  - **Substitui o Lombok `@Value`:** Elimina a dependência de plugins de bytecode de terceiros.

---

### 1.3. Sealed Classes e Interfaces (Java 17) — JEP 409
- **O que são?**  
  Classes ou interfaces que restringem explicitamente quais outras classes podem estendê-las ou implementá-las usando a cláusula `permits`.
- **Aplicação no Setor Financeiro:**  
  Modelagem de resultados de operações monetárias onde todos os desfechos possíveis são conhecidos e finitos:
  ```java
  public sealed interface TransactionResult permits Success, InsufficientBalance, AccountBlocked {}

  public record Success(String txCode, BigDecimal balance) implements TransactionResult {}
  public record InsufficientBalance(BigDecimal requested, BigDecimal available) implements TransactionResult {}
  public record AccountBlocked(String reason) implements TransactionResult {}
  ```

---

### 1.4. Pattern Matching for Switch (Java 21) — JEP 441
- **Como combina com Sealed Classes?**  
  Permite avaliar tipos em expressões `switch` com checagem de exaustividade em tempo de compilação (**sem precisar de bloco `default`**):
  ```java
  String statusMessage = switch (result) {
      case Success s -> "Transação aprovada com código: " + s.txCode();
      case InsufficientBalance b -> "Saldo insuficiente. Faltam: " + b.requested().subtract(b.available());
      case AccountBlocked a -> "Operação negada. Conta bloqueada: " + a.reason();
  };
  ```
  Se amanhã for adicionada uma nova classe no `permits`, o compilador apontará erro em todos os `switch`, impedindo bugs em produção.
- **Pattern Matching com Guardas (`when`):**
  ```java
  switch (tx) {
      case Transaction t when t.getAmount().compareTo(new BigDecimal("50000")) > 0 ->
          log.warn("Alerta COAF: Operação de grande porte detectada!");
      case Transaction t ->
          log.info("Operação padrão processada.");
  }
  ```

---

### 1.5. Sequenced Collections (Java 21) — JEP 431
- **O que resolveu?**  
  Antes do Java 21, acessar o primeiro ou último elemento de uma coleção era inconsistente: `list.get(0)`, `linkedHashSet.iterator().next()`, `sortedSet.first()`.
- **Novas interfaces unificadas:**  
  `SequencedCollection`, `SequencedSet`, `SequencedMap`.
  - `collection.getFirst()` e `collection.getLast()`
  - `collection.addFirst(e)` e `collection.addLast(e)`
  - `collection.reversed()`: visão invertida sem duplicar a lista em memória.
- **Exemplo Bancário:** Obter o lançamento mais recente do extrato bancário com `statement.getFirst()`.

---

### 1.6. Garbage Collectors: G1GC vs ZGC (Generational ZGC no Java 21)
- **G1GC (Garbage-First):** Coletor padrão do Java. Equilíbrio entre throughput e pausas (*stop-the-world* na casa de dezenas a centenas de milissegundos).
- **ZGC (Z Garbage Collector):** Coletor de latência ultra-baixa.
- **O que mudou no Java 21 (Generational ZGC - JEP 439)?**  
  O ZGC agora separa a memória em gerações (*Young* e *Old*), respeitando a hipótese fraca geracional (a maioria dos objetos bancários transitórios, como DTOs e JSONs, morre jovem).
  - **Pausas Stop-the-World:** **Menores que 1 milissegundo**, mesmo com heaps de centenas de gigabytes!
  - **Ideal para:** Sistemas de pagamentos instantâneos (PIX, cartões de crédito) com SLA estrito de milissegundos.

---

# 🚀 PARTE 2: SPRING BOOT 3 E ECOSSISTEMA SPRING

### 2.1. O que mudou do Spring Boot 2 para o Spring Boot 3?
1. **Linha de Base Java:** Requer no mínimo **Java 17** (com suporte de primeira classe ao Java 21).
2. **Namespace Jakarta EE 10:** Mudança obrigatória de todos os imports `javax.*` para `jakarta.*` (ex.: `jakarta.persistence.*`, `jakarta.validation.*`, `jakarta.servlet.*`).
3. **Compilação AOT (Ahead-of-Time) & GraalVM:** Capacidade de compilar aplicações Spring Boot em executáveis nativos com inicialização em 30 milissegundos e consumo ínfimo de memória RAM.
4. **RFC 7807 (Problem Details):** Suporte nativo para padronização de erros HTTP via `org.springframework.http.ProblemDetail`.
5. **Observabilidade Unificada:** Substituição do Spring Cloud Sleuth pelo **Micrometer Tracing**.

---

### 2.2. Concorrência Bancária: Optimistic Locking vs Pessimistic Locking
- **O Problema da Race Condition:**  
  Dois clientes tentam sacar simultaneamente da mesma conta com saldo de R$ 1.000,00.
  Requisição A lê R$ 1.000,00. Requisição B lê R$ 1.000,00.  
  Requisição A saca R$ 800,00 e grava saldo R$ 200,00.  
  Requisição B saca R$ 800,00 e grava saldo R$ 200,00 (Lost Update). O banco perdeu R$ 600,00!
- **Solução 1: Optimistic Locking (Adotada no MAP-Bank)**  
  Adiciona `@Version private Long version;` na entidade `Account`.
  A query de atualização no banco vira:
  ```sql
  UPDATE tb_accounts SET balance = 200.00, version = version + 1
  WHERE id = 1 AND version = 0;
  ```
  Se a Requisição A passar primeiro, a `version` vai para 1. Quando a Requisição B tentar atualizar esperando `version = 0`, nenhuma linha será alterada (linhas afetadas = 0). O Hibernate detecta e lança imediatamente `OptimisticLockingFailureException`.
  - **Vantagem:** Altíssima performance em cenários de alta leitura e concorrência moderada.
- **Solução 2: Pessimistic Locking (`SELECT ... FOR UPDATE`)**  
  A primeira requisição bloqueia a linha no PostgreSQL até o fim do commit da transação.
  - **Vantagem:** Evita reprocessamentos em cenários de concorrência extrema de escrita.

---

# 📊 PARTE 3: OBSERVABILIDADE E MONITORAMENTO CORPORATIVO

### 3.1. Os Três Pilares da Observabilidade
1. **Métricas (Metrics):** Dados numéricos agregáveis ao longo do tempo (ex.: contadores de transações, tempo de resposta, uso de CPU).
2. **Logs Estruturados:** Registros detalhados de eventos com contexto (`timestamp`, `level`, `traceId`, `spanId`, `mensagem`).
3. **Rastreamento Distribuído (Distributed Tracing):** Visão cronológica do caminho que uma requisição percorre através dos microsserviços.

---

### 3.2. Micrometer vs Spring Boot Actuator
- **Actuator:** Fornece os endpoints HTTP/JMX de gerenciamento operacional (`/actuator/health`, `/actuator/metrics`, `/actuator/info`, `/actuator/prometheus`).
- **Micrometer:** É a "SLF4J das métricas". Uma fachada agnóstica que instrumenta seu código Java com contadores, timers e gauges, e os traduz para diversos backends (Prometheus, Datadog, New Relic, CloudWatch).

---

### 3.3. Instrumentação de Negócio no MAP-Bank
No `TransactionService`, utilizamos o `MeterRegistry` para injetar métricas personalizadas:
- `map.bank.pix.transfers.total`: Contador de transferências PIX concluídas.
- `map.bank.internal.transfers.total`: Contador de TED/TEF internas.
- `map.bank.transactions.failed.total`: Contador de falhas (fraude, limite, saldo insuficiente).
- `map.bank.transaction.duration`: Timer que calcula os percentis P50, P95 e P99 de latência.

---

### 3.4. Distributed Tracing: TraceId e SpanId
- **TraceId:** Um identificador hexadecimal único (ex.: `c89b21f3a4e9b812`) gerado na entrada da requisição HTTP que permanece inalterado por todo o ciclo de vida da transação em todos os microsserviços.
- **SpanId:** Identifica uma etapa específica dentro do fluxo (ex.: tempo gasto na consulta ao banco ou na chamada ao DICT do BACEN).
- **Como configuramos nos logs do MAP-Bank:**
  ```yaml
  logging:
    pattern:
      level: "%5p [map-bank,%X{traceId:-},%X{spanId:-}]"
  ```
  Isso permite que você abra o Grafana Loki, Kibana ou CloudWatch, digite o `traceId` impresso no comprovante do cliente e veja exatamente a linha onde ocorreu o erro, milissegundo a milissegundo.

---

# 🗄️ PARTE 4: SQL, POSTGRESQL E PADRÕES BANCÁRIOS

### 4.1. Níveis de Isolamento de Transações (ANSI SQL)
| Nível de Isolamento | Dirty Read | Non-Repeatable Read | Phantom Read | Padrão PostgreSQL |
| :--- | :---: | :---: | :---: | :---: |
| **Read Uncommitted** | Sim | Sim | Sim | Não suportado (age como Read Committed) |
| **Read Committed** | **Não** | Sim | Sim | **SIM (Padrão do Postgres)** |
| **Repeatable Read** | **Não** | **Não** | Sim (Postgres bloqueia) | Sob demanda |
| **Serializable** | **Não** | **Não** | **Não** | Isolamento total (mais lento) |

- **No MAP-Bank:** Usamos `@Transactional(isolation = Isolation.READ_COMMITTED)` combinado com `@Version`, obtendo isolamento seguro sem travar o banco.

---

### 4.2. Estratégia de Migrations com Flyway
- Por que nunca usar `hibernate.hbm2ddl.auto = update` em bancos corporativos?
  - Não há histórico de quem aplicou a alteração.
  - Pode apagar dados inadvertidamente ao renomear colunas.
  - Não funciona em pipelines de CI/CD automatizadas.
- O Flyway cria a tabela `flyway_schema_history` e valida o checksum de cada script SQL versionado (`V1__...sql`, `V2__...sql`), garantindo paridade absoluta entre Desenvolvimento, Homologação e Produção.

---

# 🎤 SIMULAÇÃO DA SABATINA TÉCNICA (PERGUNTAS REAIS DA BANCA NTT DATA)

### Q1: "Por que você escolheu Java 21 para este projeto em vez do Java 17?"
> **Sua Resposta:**  
> *"Embora o Java 17 seja uma excelente versão LTS que introduziu Sealed Classes e consolidou Records, o Java 21 trouxe a maior evolução de concorrência na história da plataforma: as **Virtual Threads (Project Loom - JEP 444)**. Para sistemas bancários como o MAP-Bank, onde grande parte das operações consiste em I/O bloqueante (consultas JDBC no PostgreSQL e requisições HTTP para autorizadores externos), as Virtual Threads permitem escalar para dezenas de milhares de requisições simultâneas sem esgotar o pool de threads do sistema operacional e sem o custo cognitivo da programação reativa. Além disso, o Java 21 introduziu o **Generational ZGC**, reduzindo pausas de garbage collection para menos de 1ms, o que é crucial para SLAs bancários de PIX."*

### Q2: "Como você lidou com concorrência no saldo bancário de uma conta?"
> **Sua Resposta:**  
> *"No MAP-Bank, apliquei o padrão de **Optimistic Locking** através da anotação `@Version` na entidade `Account`. A cada transação, o Hibernate valida a versão do registro na cláusula WHERE do update. Se duas transações de débito concorrentes tentarem modificar a mesma conta ao mesmo tempo, a primeira é persistida e a segunda é abortada com `OptimisticLockingFailureException`. Capturei essa exceção no meu `GlobalExceptionHandler`, retornando HTTP 409 Conflict padronizado no formato RFC 7807 (Problem Details). Caso o cenário exigisse concorrência extrema onde o cliente não pode retentar, uma alternativa viável seria o Pessimistic Lock via `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT FOR UPDATE`)."*

### Q3: "Como você garantiu a observabilidade do serviço?"
> **Sua Resposta:**  
> *"Implementei os três pilares da observabilidade:  
> 1. **Métricas:** Integrei o Spring Boot Actuator com Micrometer e exportador para Prometheus (`/actuator/prometheus`). Além das métricas padrão de JVM e Tomcat, criei métricas customizadas de negócio como `map.bank.pix.transfers.total` e um Timer para monitorar os percentis P95 e P99 do tempo de resposta.  
> 2. **Distributed Tracing:** Utilizei o `micrometer-tracing-bridge-brave` para propagação de contexto W3C e injeção automática de `traceId` e `spanId` no MDC (Mapped Diagnostic Context) do Logback.  
> 3. **Health Checks Corporativos:** Criei um `HealthIndicator` customizado reportando o status de conectividade dos gateways regulatórios (SPI e BACEN DICT)."*

### Q4: "Como funciona a idempotência que você implementou no PIX e transferências?"
> **Sua Resposta:**  
> *"Operações financeiras críticas não podem ser reprocessadas caso haja um timeout de rede entre o cliente e o banco. No MAP-Bank, permitimos o envio de um header `Idempotency-Key` (UUID). No `TransactionService`, antes de debitar a conta, verificamos se já existe uma transação gravada com esse identificador único. Se existir, o sistema não debita novamente a conta e devolve imediatamente o comprovante original já liquidado, prevenindo pagamentos em duplicidade."*
