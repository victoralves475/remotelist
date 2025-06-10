package br.edu.ifpb.remotelist;

import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Duration;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        var impl = new RemoteListImpl(
                Paths.get("data", "operations.log"),
                Duration.ofMinutes(1)
        );
        Registry reg = LocateRegistry.createRegistry(1099);
        reg.rebind("RemoteListService", impl);
        System.out.println("Servidor RMI pronto em localhost:1099");
    }
}
