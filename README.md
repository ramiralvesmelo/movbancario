# Processamento de Arquivos CNAB

## Visão Geral

O processamento de arquivos CNAB foi projetado para operar de forma assíncrona utilizando JMS/MDB, garantindo:

* Processamento distribuído
* Controle de concorrência via File System Lock
* Persistência temporária em Redis
* Persistência final em banco H2 utilizando JDBC Batch
* Rastreabilidade completa do processamento
* Controle de reprocessamento
* Auditoria operacional

---

# Arquitetura Geral

```mermaid
flowchart TD

    SCHED[MyScheduler<br/>Execução a cada 5 minutos]

    JMSPRODUCER[Produtor JMS<br/>Localiza Arquivos CNAB]

    JMSQUEUE[JMS Queue<br/>cargaArquivoMovimentoBancario]

    MDB[Message Driven Bean MDB]

    LOCK[File System Lock<br/>arquivo.rem.lock]

    VALIDAARQ[Validação Arquivo<br/>Reprocessamento]

    REDISCTRL[Controle Redis]

    STREAM[Leitura Streaming]

    CNAB[Validação CNAB240]

    VALIDO[Registros Válidos]

    INVALIDO[Registros Inválidos]

    REDISVALIDO[(Redis Válidos)]

    REDISINVALIDO[(Redis Inválidos)]

    RECUPERA[Recupera Registros<br/>Válidos Redis]

    TRANSFORMA[Transforma Linha CNAB<br/>em RegistroCnab]

    H2[Persistência JDBC Batch H2]

    FINALIZA[Finaliza Processamento]

    SCHED --> JMSPRODUCER

    JMSPRODUCER --> JMSQUEUE

    JMSQUEUE --> MDB

    MDB --> LOCK

    LOCK --> VALIDAARQ

    VALIDAARQ --> REDISCTRL

    REDISCTRL --> STREAM

    STREAM --> CNAB

    CNAB -->|Válido| VALIDO

    CNAB -->|Inválido| INVALIDO

    VALIDO --> REDISVALIDO

    INVALIDO --> REDISINVALIDO

    REDISVALIDO --> RECUPERA

    RECUPERA --> TRANSFORMA

    TRANSFORMA --> H2

    H2 --> FINALIZA

    FINALIZA --> REDISCTRL
```
---


jdbc:h2:file:c:/tmp/movbancario/movbancario


<datasource jndi-name="java:jboss/datasources/ExampleDS" pool-name="ExampleDS" enabled="true" use-java-context="true">
    <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE</connection-url>
    <driver>h2</driver>
    <security>
        <user-name>sa</user-name>
        <password>sa</password>
    </security>
</datasource>
<datasource jndi-name="java:/jdbc/MovBancarioDS" pool-name="MovBancarioDS" enabled="true" use-java-context="true">
    <connection-url>jdbc:h2:file:/tmp/movbancario/movbancario;MULTI_THREADED=TRUE</connection-url>
    <driver>h2</driver>
    <security>
        <user-name>sa</user-name>
        <password>sa</password>
    </security>
</datasource>

