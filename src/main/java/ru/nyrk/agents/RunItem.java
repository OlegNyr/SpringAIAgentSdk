package ru.nyrk.agents;

/**
 * Описывает действие которое произошло с LLM, для передачи преобразуем в ResponseInputItem
 */
public interface RunItem<T> {
    Agent getAgent();

    T getRawItem();

    /**
     * @return
     */
    ResponseInputItem makeInputItem();
}