package br.edu.ifpb.remotelist;

import br.edu.ifpb.remotelist.controller.RemoteListController;
import br.edu.ifpb.remotelist.model.ListService;
import br.edu.ifpb.remotelist.persistence.FilePersistenceManager;
import br.edu.ifpb.remotelist.persistence.PersistenceManager;

import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.time.Duration;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Ponto de entrada do servidor:
 * - inicializa ListService e PersistenceManager
 * - recupera estado
 * - registra serviço RMI
 */
public class ServerMain {
    public static void main(String[] args) throws Exception {
        // 1) Define onde os arquivos de log e snapshot serão armazenados
        var dataDir = Paths.get("data");

        // 2) Trava que garante leitura consistente durante o snapshot
        ReadWriteLock snapshotLock = new ReentrantReadWriteLock();

        // 3) Intervalo entre snapshots (aqui, a cada 1 minuto)
        Duration snapshotInterval = Duration.ofMinutes(1);

        // 4) Cria a camada de negócio: o serviço que mantém as listas em memória
        ListService listService = new ListService();

        // 5) Cria o gerenciador de persistência: grava log e tira snapshots
        PersistenceManager persistence =
                new FilePersistenceManager(
                        listService,       // objeto que sabe o estado em memória
                        dataDir,           // diretório onde ficam os arquivos
                        snapshotInterval,  // com que frequência tirar snapshots
                        snapshotLock       // trava para coordenar snapshot vs operações
                );

        // 6) Recupera estado antigo (se houver snapshot + log gravados)
        persistence.recover(listService);

        // 7) Cria o “controller” RMI, que expõe os métodos remotos
        RemoteListController controller =
                new RemoteListController(listService, persistence);

        // 8) Sobe o Registry RMI na porta 1099 e registra o serviço
        LocateRegistry
                .createRegistry(1099)
                .rebind("RemoteListService", controller);

        // 9) Confirma no console que o servidor está pronto
        System.out.println("Servidor RemoteList pronto em localhost:1099");
    }
}

