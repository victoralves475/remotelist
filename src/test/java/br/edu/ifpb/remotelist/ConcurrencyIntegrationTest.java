package br.edu.ifpb.remotelist;

import br.edu.ifpb.remotelist.controller.RemoteListController;
import br.edu.ifpb.remotelist.model.ListService;
import br.edu.ifpb.remotelist.persistence.FilePersistenceManager;
import br.edu.ifpb.remotelist.persistence.PersistenceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de concorrência para simular múltiplos clientes acessando o sistema simultaneamente.
 */
public class ConcurrencyIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void testConcurrentAppendAndRemoveOnController() throws Exception {
        // Configurações iniciais
        ListService service = new ListService();
        PersistenceManager persistence = new FilePersistenceManager(
                service,
                tempDir,
                Duration.ofDays(1),          // snapshot distante para não interferir
                new ReentrantReadWriteLock());
        // recover inicia estado vazio
        persistence.recover(service);

        RemoteListController controller = new RemoteListController(service, persistence);
        String listId = "concurrent";

        int numThreads = 10;
        int opsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Cada thread faz ops: append de valores e remove algumas vezes
        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    // 1) append
                    for (int i = 0; i < opsPerThread; i++) {
                        controller.append(listId, 1);
                    }
                    // 2) remove metade
                    for (int i = 0; i < opsPerThread / 2; i++) {
                        controller.remove(listId);
                    }
                } catch (Exception e) {
                    // falhas não esperadas
                    fail("Exception during concurrent ops: " + e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // Esperamos que cada thread tenha deixado opsPerThread/2 elementos,
        // ou seja, total = numThreads * (opsPerThread - opsPerThread/2)
        int expected = numThreads * (opsPerThread / 2);
        int actual = controller.size(listId);
        assertEquals(expected, actual,
                "Esperado size=" + expected + " mas foi=" + actual);
    }
}
