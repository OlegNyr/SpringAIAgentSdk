package ru.nyrk.agents.item.input;

import lombok.Builder;
import lombok.Value;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.item.Role;

/**
 * Запрос вызова функции агента
 */
@Builder
@Value
public class ResponseOutputMessageParam implements ResponseInputItem {
    /**
     * Имя функции
     */
    String name;

    /**
     * Уникальный идентификатор вызова функционального инструмента, сгенерированный моделью.
     */
    String callId;

    /**
     * Аргументы вызова функции
     */
    String arguments;
}
