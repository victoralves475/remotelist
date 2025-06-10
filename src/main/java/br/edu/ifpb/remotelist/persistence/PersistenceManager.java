package br.edu.ifpb.remotelist.persistence;

import br.edu.ifpb.remotelist.model.ListService;
import java.io.IOException;

/**
 * Define operações de persistência para o RemoteList:
 * - registrar append e remove no log
 * - recuperar estado via log e snapshot
 */
public interface PersistenceManager {
    /**
     * Grava no log que foi realizado um append na lista.
     * @param listId identificador da lista
     * @param value valor adicionado
     */
    void recordAppend(String listId, int value);

    /**
     * Grava no log que foi realizado um remove na lista.
     * @param listId identificador da lista
     */
    void recordRemove(String listId);

    /**
     * Recupera o estado inicial das listas, carregando snapshot e replay do log.
     * @param listService serviço de listas para carregar estado
     */
    void recover(ListService listService) throws IOException;
}