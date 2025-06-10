package br.edu.ifpb.remotelist.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentMap;

/**
 * Serviço de gerenciamento de listas em memória.
 * Responsável por todas as regras de negócio sobre listas.
 */
public class ListService {
    private final ConcurrentMap<String, CopyOnWriteArrayList<Integer>> lists = new ConcurrentHashMap<>();

    /**
     * Carrega uma lista existente no estado inicial (snapshot).
     * @param listId identificador da lista
     * @param data   dados previamente salvos
     */
    public void loadList(String listId, List<Integer> data) {
        CopyOnWriteArrayList<Integer> listCopy = new CopyOnWriteArrayList<>(data);
        lists.put(listId, listCopy);
    }

    /**
     * Adiciona um valor ao final da lista.
     */
    public void append(String listId, int value) {
        lists.computeIfAbsent(listId, id -> new CopyOnWriteArrayList<>()).add(value);
    }

    /**
     * Retorna o valor na posição 'index' da lista.
     */
    public int get(String listId, int index) {
        List<Integer> list = lists.getOrDefault(listId, new CopyOnWriteArrayList<>());
        return list.get(index);
    }

    /**
     * Remove e retorna o último elemento da lista.
     */
    public int remove(String listId) {
        CopyOnWriteArrayList<Integer> list = lists.getOrDefault(listId, new CopyOnWriteArrayList<>());
        if (list.isEmpty()) {
            throw new IllegalStateException("Lista vazia");
        }
        return list.remove(list.size() - 1);
    }

    /**
     * Retorna o tamanho atual da lista.
     */
    public int size(String listId) {
        return lists.getOrDefault(listId, new CopyOnWriteArrayList<>()).size();
    }

    /**
     * Fornece uma cópia imutável do estado de todas as listas para o snapshot.
     */
    public Map<String, List<Integer>> getSnapshot() {
        Map<String, List<Integer>> snapshot = new HashMap<>();
        for (Map.Entry<String, CopyOnWriteArrayList<Integer>> entry : lists.entrySet()) {
            // CopyOnWriteArrayList é thread-safe, mas ainda copiamos para evitar alterações
            snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return Collections.unmodifiableMap(snapshot);
    }
}
