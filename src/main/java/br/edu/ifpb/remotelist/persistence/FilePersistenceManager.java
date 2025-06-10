package br.edu.ifpb.remotelist.persistence;

import br.edu.ifpb.remotelist.model.ListService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * Implementação de PersistenceManager que grava operações em log
 * e gera snapshots periódicos para recuperação rápida.
 */
public class FilePersistenceManager implements PersistenceManager {
    private final ListService listService;
    private final Path logPath;
    private final Path snapshotPath;
    private final ReadWriteLock snapshotLock;
    private final ScheduledExecutorService scheduler;
    private final Gson gson = new Gson();

    /**
     * @param listService serviço de listas para carregar estado e gerar snapshot
     * @param dataDir diretório onde estão log e snapshot
     * @param snapshotInterval intervalo entre snapshots
     * @param snapshotLock trava para garantir leitura consistente do estado
     */
    public FilePersistenceManager(ListService listService,
                                  Path dataDir,
                                  Duration snapshotInterval,
                                  ReadWriteLock snapshotLock) throws IOException {
        this.listService = Objects.requireNonNull(listService, "ListService não pode ser nulo");
        this.logPath = dataDir.resolve("operations.log");
        this.snapshotPath = dataDir.resolve("snapshot.json");
        Files.createDirectories(dataDir);
        this.snapshotLock = snapshotLock;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        // agenda snapshot periódico
        scheduler.scheduleAtFixedRate(this::takeSnapshot,
                snapshotInterval.toMillis(),
                snapshotInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordAppend(String listId, int value) {
        writeLog(String.format("APPEND %s %d%n", listId, value));
    }

    @Override
    public void recordRemove(String listId) {
        writeLog(String.format("REMOVE %s%n", listId));
    }

    private synchronized void writeLog(String entry) {
        try {
            Files.writeString(logPath, entry,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void recover(ListService service) throws IOException {
        // 1) carregar snapshot
        if (Files.exists(snapshotPath)) {
            Type type = new TypeToken<Map<String, List<Integer>>>(){}.getType();
            try (Reader reader = Files.newBufferedReader(snapshotPath)) {
                Map<String, List<Integer>> state = gson.fromJson(reader, type);
                state.forEach(service::loadList);
            }
        }
        // 2) replay do log
        if (Files.exists(logPath)) {
            try (BufferedReader br = Files.newBufferedReader(logPath)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(" ");
                    switch (parts[0]) {
                        case "APPEND":
                            service.append(parts[1], Integer.parseInt(parts[2]));
                            break;
                        case "REMOVE":
                            service.remove(parts[1]);
                            break;
                    }
                }
            }
        }
    }

    /**
     * Cria um snapshot consistente do estado e limpa o log.
     */
    private void takeSnapshot() {
        snapshotLock.readLock().lock();
        try {
            // copia estado atual em memória
            Map<String, List<Integer>> copy = listService.getSnapshot();
            // escreve snapshot em arquivo temporário
            Path tmp = snapshotPath.resolveSibling("snapshot.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp)) {
                gson.toJson(copy, w);
            }
            // renomeia para arquivo definitivo (atômico)
            Files.move(tmp, snapshotPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            // trunca log para zero bytes
            try (OutputStream os = Files.newOutputStream(
                    logPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                // apenas abre e fecha
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            snapshotLock.readLock().unlock();
        }
    }
}
