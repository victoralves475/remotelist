package br.edu.ifpb.remotelist.controller;

import br.edu.ifpb.remotelist.model.ListService;
import br.edu.ifpb.remotelist.persistence.PersistenceManager;
import br.edu.ifpb.remotelist.backup.RemoteList;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Controller RMI que expõe o serviço de listas para clientes remotos.
 * Recebe chamadas RPC, delega à lógica de negócio e persiste operações.
 */
public class RemoteListController extends UnicastRemoteObject implements RemoteList {
    private final ListService listService;
    private final PersistenceManager persistence;

    public RemoteListController(ListService listService, PersistenceManager persistence) throws RemoteException {
        super();
        this.listService = listService;
        this.persistence = persistence;
    }

    @Override
    public void append(String listId, int value) throws RemoteException {
        listService.append(listId, value);
        persistence.recordAppend(listId, value);
    }

    @Override
    public int get(String listId, int index) throws RemoteException {
        return listService.get(listId, index);
    }

    @Override
    public int remove(String listId) throws RemoteException {
        int removed = listService.remove(listId);
        persistence.recordRemove(listId);
        return removed;
    }

    @Override
    public int size(String listId) throws RemoteException {
        return listService.size(listId);
    }
}
