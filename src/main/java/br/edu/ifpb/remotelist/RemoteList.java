package br.edu.ifpb.remotelist;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteList extends Remote {

    void append(String listId, int value) throws RemoteException;
        // Append(list_id, v)
        // → coloca o valor v no final da lista com identificador list_id

    int get(String listId, int index) throws RemoteException, IndexOutOfBoundsException;
        // Get(list_id, i)
        // → retorna o valor da posição i, na lista com identificador list_id

    int remove(String listId) throws RemoteException, IllegalStateException;
        // Remove(list_id)
        // → remove e retorna o último elemento da lista com identificador list_id

    int size(String listId) throws RemoteException;
        // Size(list_id)
        // → obtém a quantidade de elementos armazenados na lista com identificador list_id

}
