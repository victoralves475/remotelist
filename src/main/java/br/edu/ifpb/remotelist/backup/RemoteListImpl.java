package br.edu.ifpb.remotelist.backup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class RemoteListImpl extends UnicastRemoteObject implements RemoteList {
    private final ConcurrentMap<String, List<Integer>> lists = new ConcurrentHashMap<>();
    private final ReadWriteLock mapLock = new ReentrantReadWriteLock();
    private final Path logPath;
    public final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Gson gson = new Gson();

    public RemoteListImpl(Path logPath, Duration snapshotInterval) throws IOException {
        super();
        this.logPath = logPath;
        Files.createDirectories(logPath.getParent());
        recoverState();
        // agenda snapshot periódico
        scheduler.scheduleAtFixedRate(this::takeSnapshot,
                snapshotInterval.toMillis(),
                snapshotInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void append(String listId, int value) throws RemoteException {
        List<Integer> lst = lists.computeIfAbsent(listId, k -> new ArrayList<>());
        synchronized (lst) {
            lst.add(value);
            writeLog(String.format("APPEND %s %d%n", listId, value));
        }
    }

    @Override
    public int get(String listId, int index) throws RemoteException {
        List<Integer> lst = lists.getOrDefault(listId, Collections.emptyList());
        synchronized (lst) {
            return lst.get(index);
        }
    }

    @Override
    public int remove(String listId) throws RemoteException {
        List<Integer> lst = lists.getOrDefault(listId, Collections.emptyList());
        synchronized (lst) {
            if (lst.isEmpty()) throw new IllegalStateException("Lista vazia");
            int v = lst.remove(lst.size() - 1);
            writeLog(String.format("REMOVE %s%n", listId));
            return v;
        }
    }

    @Override
    public int size(String listId) throws RemoteException {
        List<Integer> lst = lists.getOrDefault(listId, Collections.emptyList());
        synchronized (lst) {
            return lst.size();
        }
    }

    private synchronized void writeLog(String entry) {
        try {
            Files.writeString(logPath, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void recoverState() throws IOException {
        // 1) carregar snapshot.json (se existir)
        Path snap = logPath.getParent().resolve("snapshot.json");
        if (Files.exists(snap)) {
            Type type = new TypeToken<Map<String, List<Integer>>>(){}.getType();
            Map<String, List<Integer>> recovered;
            try (Reader r = Files.newBufferedReader(snap)) {
                recovered = gson.fromJson(r, type);
            }
            lists.putAll(recovered);
        }
        // 2) aplicar operações do log
        if (Files.exists(logPath)) {
            try (BufferedReader br = Files.newBufferedReader(logPath)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.split(" ");
                    switch (p[0]) {
                        case "APPEND" ->
                                lists.computeIfAbsent(p[1], k -> new ArrayList<>())
                                        .add(Integer.parseInt(p[2]));
                        case "REMOVE" -> {
                            List<Integer> lst = lists.get(p[1]);
                            if (lst != null && !lst.isEmpty()) lst.remove(lst.size() - 1);
                        }
                    }
                }
            }
        }
    }

    private void takeSnapshot() {
        // 1) copia o estado atual
        Map<String, List<Integer>> copy = new HashMap<>();
        mapLock.readLock().lock();
        try {
            for (var e : lists.entrySet()) {
                synchronized (e.getValue()) {
                    copy.put(e.getKey(), new ArrayList<>(e.getValue()));
                }
            }
        } finally {
            mapLock.readLock().unlock();
        }

        // 2) escreve snapshot em arquivo temporário
        Path tmp = logPath.getParent().resolve("snapshot.json.tmp");
        Path snap = logPath.getParent().resolve("snapshot.json");
        try (Writer w = Files.newBufferedWriter(tmp)) {
            gson.toJson(copy, w);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        // 3) move para o arquivo definitivo (atomicamente)
        try {
            Files.move(tmp, snap, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        // 4) TRUNCA o log original para zero bytes
        synchronized (this) {
            try (OutputStream os = Files.newOutputStream(
                    logPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                // só abre e fecha, isso limpa o arquivo
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


//    private void takeSnapshot() {
//        Map<String, List<Integer>> copy = new HashMap<>();
//        mapLock.readLock().lock();
//        try {
//            for (var e : lists.entrySet()) {
//                synchronized (e.getValue()) {
//                    copy.put(e.getKey(), new ArrayList<>(e.getValue()));
//                }
//            }
//        } finally {
//            mapLock.readLock().unlock();
//        }
//
//        Path tmp = logPath.getParent().resolve("snapshot.json.tmp");
//        Path snap = logPath.getParent().resolve("snapshot.json");
//        try (Writer w = Files.newBufferedWriter(tmp)) {
//            gson.toJson(copy, w);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            return;
//        }
//        try {
//            Files.move(tmp, snap, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
}