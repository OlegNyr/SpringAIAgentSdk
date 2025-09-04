package ru.nyrk.agents.models;

import lombok.Builder;
import lombok.Value;
import ru.nyrk.agents.ResponseOutputItem;

/**
 * Возврат модели для вызова функции
 */
@Value
@Builder
public class ResponseFunctionToolCall implements ResponseOutputItem {

    /**
     * Строка JSON с аргументами для передачи в функцию.
     */
    String arguments;

    /**
     * Уникальный идентификатор вызова функционального инструмента, сгенерированный моделью.
     */
    String callId;

    /**
     * Имя функции, которую нужно запустить.
     */
    String name;
}
