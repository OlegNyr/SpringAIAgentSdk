package ru.nyrk.agents.item.input;

import lombok.Builder;
import lombok.Value;
import ru.nyrk.agents.ResponseInputItem;

@Value
@Builder
public class FunctionCallOutput implements ResponseInputItem {

    /**
     * Уникальный идентификатор вызова функционального инструмента, сгенерированный моделью.
     */
    String callId;

    /**
     * Имя функции
     */
    String name;

    /**
     * Результат работы функции
     */
    String result;

}
