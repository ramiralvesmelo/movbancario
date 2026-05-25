# Análise de Performance JVM - ETL CNAB



## Objetivo

Documentar os procedimentos de análise de performance, consumo de memória, GC e geração de heap dump durante o processamento ETL CNAB.


/**
 * Serviço responsável pelo processamento
 * streaming dos arquivos CNAB do
 * movimento bancário.
 *
 * Fluxo operacional:
 *
 * Agendador
 *      ↓
 * Controle de execução única
 *      ↓
 * Leitura streaming do arquivo
 *      ↓
 * Normalização da linha
 *      ↓
 * Validação estrutural/layout
 *      ↓
 * Controle de duplicidade
 *      ↓
 * Separação lógica:
 *      - registros válidos
 *      - registros inválidos
 *      ↓
 * Persistência temporária Redis
 *      ↓
 * Recuperação batch dos registros válidos
 *      ↓
 * Transformação para DTO
 *      ↓
 * Persistência JDBC batch
 *      ↓
 * Commit transacional por lote
 *      ↓
 * Atualização checkpoint/log operacional
 *      ↓
 * Limpeza recursos temporários Redis
 *      ↓
 * Finalização
 *
 * Estratégia operacional:
 *
 * - processamento streaming
 * - baixo consumo memória
 * - fail-fast para registros inválidos
 * - tolerância a falhas
 * - rastreabilidade operacional
 * - prevenção de duplicidade
 * - persistência temporária Redis
 * - separação de registros inválidos
 * - processamento batch JDBC
 * - preservação da ordem posicional CNAB
 * - redução de consumo heap/JVM
 *
 * Caso o registro já exista:
 *
 * - a linha é ignorada
 * - não é persistida novamente
 * - evita reprocessamento duplicado
 * - preserva integridade operacional
 *
 * Benefícios:
 *
 * - reprocessamento controlado
 * - troubleshooting operacional
 * - observabilidade
 * - resiliência do ETL
 * - desacoplamento entre leitura e persistência
 * - prevenção de inconsistência por duplicidade
 */

---

# Configuração JVM Recomendada

## Java 8+

```bash
-Xms2g
-Xmx2g
-XX:+UseG1GC
-XX:+HeapDumpOnOutOfMemoryError
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/tmp/gc.log
```

---

# Explicação dos Parâmetros

| Parâmetro | Objetivo |
|---|---|
| `-Xms2g` | Heap inicial |
| `-Xmx2g` | Heap máximo |
| `-XX:+UseG1GC` | Garbage Collector G1 |
| `-XX:+HeapDumpOnOutOfMemoryError` | Gera heap dump automático em OOM |
| `-XX:+PrintGCDetails` | Log detalhado GC |
| `-XX:+PrintGCDateStamps` | Timestamp GC |
| `-Xloggc:/tmp/gc.log` | Arquivo log GC |

---

# Análise GC

## Ferramenta recomendada

GCViewer

https://github.com/chewiebug/GCViewer

---

# Execução GCViewer

```bat
java -jar gcviewer-1.36.jar
```

---

# Métricas Importantes

| Métrica | Esperado |
|---|---|
| Throughput JVM | > 95% |
| Full GC | mínimo |
| Pause Time | baixa |
| Heap After GC | estável |

---

# Geração Heap Dump Manual

## Descobrir PID JVM

```bash
jps -l
```

Exemplo:

```text
25816
```

---

# Gerar Heap Dump

```bat
jcmd 25816 GC.heap_dump C:\tmp\cnab-etl.hprof
```

---

# Quando Gerar o Dump

## Recomendado

Durante o processamento do ETL.

Objetivo:

- capturar objetos vivos
- identificar retenção memória
- analisar batches
- validar comportamento streaming

---

# Análise Heap Dump

## Ferramenta recomendada

Eclipse Memory Analyzer (MAT)

https://eclipse.dev/mat/

---

# Principais Análises MAT

## Leak Suspects Report

Identifica possíveis vazamentos memória.

---

## Dominator Tree

Mostra quem está retendo heap.

Ordenar por:

```text
Retained Heap
```

---

## Histogram

Mostra quantidade objetos:

- String
- byte[]
- ArrayList
- RegistroCnab

---

## Path to GC Roots

Mostra porque o objeto ainda está vivo.

---

# Comportamento Esperado do ETL

## Saudável

- heap estável
- objetos temporários
- GC funcionando
- baixa retenção memória
- streaming efetivo

---

# Possíveis Problemas

| Problema | Sintoma |
|---|---|
| List sem clear() | heap crescente |
| batch excessivo | Full GC |
| retenção DTO | Old Gen |
| cache excessivo | memória alta |
| duplicidade objetos | consumo heap |

---

# Estruturas Sensíveis do ETL

## Validar comportamento:

- blocoRedis
- lote
- RegistroCnab
- String CNAB
- buffers JDBC

---

# Estratégia Atual do ETL

- processamento streaming
- batch JDBC
- persistência temporária Redis
- controle duplicidade Redis
- separação registros inválidos
- baixo consumo memória
- fail-fast validação
- preservação ordem CNAB

---

# Ferramentas Recomendadas

| Ferramenta | Objetivo |
|---|---|
| VisualVM | monitoramento realtime |
| GCViewer | análise GC |
| MAT | análise heap dump |
| JMC/JFR | profiling produção |

---

# Observação

Para ETL CNAB com grandes volumes:

- evitar carregar arquivo completo memória
- evitar listas gigantes
- utilizar batch controlado
- monitorar Full GC
- validar retenção objetos
- manter processamento streaming