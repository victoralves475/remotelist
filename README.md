# Documentação do Projeto RemoteList (Servidor)

Este repositório contém o servidor RMI do sistema **RemoteList**, responsável por gerenciar as listas de inteiros, garantir persistência e atender a múltiplos clientes simultaneamente.

## 1. Pré-requisitos

* Java JDK 21
* Maven 3.6+

## 2. Estrutura do Repositório

```
remotelist/
├─ pom.xml
└─ src/
   ├─ main/java/
   │  └─ br/edu/ifpb/remotelist/
   │     ├─ controller/        # RemoteListController (RMI)
   │     ├─ model/             # ListService (lógica de negócio)
   │     ├─ persistence/       # FilePersistenceManager e interface PersistenceManager
   │     └─ ServerMain.java    # ponto de entrada
   └─ test/java/
      └─ br/edu/ifpb/remotelist/  # testes unitários e de integração
```

## 3. Build

```bash
cd remotelist/
mvn clean package
```

O JAR do servidor será gerado em:

```
target/remotelist-server-<versão>.jar
```

## 4. Execução

```bash
java -jar target/remotelist-server-<versão>.jar
```

Por padrão:

* RMI Registry na porta **1099**
* Diretório de dados: `./data/` (criado automaticamente)
* Snapshot a cada 1 minuto

### 4.1 Argumentos Opcionais

Se houver parâmetros em `ServerMain`, pode-se passar:

```
java -jar remotelist-server.jar <porta> <data-dir> <intervalo-segundos>
```

## 5. Persistência

* **Log**: cada `append`/`remove` gravado em `operations.log`.
* **Snapshot**: arquivo JSON (`snapshot.json`) gravado periodicamente; log é truncado após snapshot.
* **Recover**: no startup, o servidor carrega o snapshot e reexecuta o log para restabelecer o estado.

## 6. Concorrência

* `ListService` usa `ConcurrentHashMap` e `CopyOnWriteArrayList` para listas thread-safe.
* `FilePersistenceManager` protege a criação de snapshot com `ReadWriteLock` e gravação de log com bloqueio (`synchronized`).

---
