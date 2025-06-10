package br.edu.ifpb.remotelist;

import br.edu.ifpb.remotelist.backup.RemoteListImpl;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class RemoteListImplTest {
    static Path baseDir;
    static RemoteListImpl impl;

    @BeforeAll
    static void setup() throws Exception {
        baseDir = Files.createTempDirectory("remotelist-test");
        impl = new RemoteListImpl(
                baseDir.resolve("operations.log"),
                Duration.ofSeconds(1)  // snapshot rápido
        );
    }

    @AfterAll
    static void teardown() {
        impl.scheduler.shutdownNow();
    }

    @Test
    void testAppendGetSizeRemove() throws Exception {
        impl.append("L", 5);
        impl.append("L", 7);
        assertEquals(2, impl.size("L"));
        assertEquals(7, impl.get("L", 1));
        assertEquals(7, impl.remove("L"));
        assertEquals(1, impl.size("L"));
    }

    @Test
    void testPersistenceAndRecovery() throws Exception {
        impl.append("R", 100);
        // aguardar snapshot e log
        TimeUnit.SECONDS.sleep(2);
        // criar nova instância simulando restart
        var newImpl = new RemoteListImpl(
                baseDir.resolve("operations.log"),
                Duration.ofSeconds(10)
        );
        assertEquals(1, newImpl.size("R"));
        assertEquals(100, newImpl.get("R", 0));
    }

    @Test
    void testConcurrency() throws Exception {
        int threads = 10, per = 100;
        ExecutorService svc = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            svc.submit(() -> {
                for (int j = 0; j < per; j++) {
                    try {
                        impl.append("C", j);
                    } catch (Exception ignored) {}
                }
            });
        }
        svc.shutdown();
        assertTrue(svc.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(threads * per, impl.size("C"));
    }
}
