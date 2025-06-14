package br.edu.ifpb.remotelist.backup;

import br.edu.ifpb.remotelist.RemoteList;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientApp {
    public static void main(String[] args) throws Exception {
        Registry reg = LocateRegistry.getRegistry("localhost", 1099);
        RemoteList rl = (RemoteList) reg.lookup("RemoteListService");

        rl.append("lista1", 10);
        rl.append("lista1", 20);
        System.out.println("Tamanho: " + rl.size("lista1"));            // 2
        System.out.println("Item[1]: " + rl.get("lista1", 1));         // 20
        System.out.println("Removido: " + rl.remove("lista1"));        // 20
    }
}
