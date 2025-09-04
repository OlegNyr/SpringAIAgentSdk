package ru.nyrk.agents;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

/// Возврат из agentRunner c финальным описанием работы
@Value
@Builder
public class RunResult<T> {

    /// Агент, который обработал последний запрос
    Agent lastAgent;

    /// Исходные входные элементы, то есть элементы до `AgentRunner.run()` были вызваны.
    /// Это может быть мутированная версия ввода, если есть входные фильтры, которые мутируют вход.
    List<ResponseInputItem> input;

    /**
     * Новые элементы, сгенерированные во время запуска агента.
     * К ним относятся такие вещи, как новые сообщения, вызовы инструментов и их выходы и т.д.
     */
    List<RunItem<?>> newItems;

    /**
     * Сырые ответы LLM, генерируемые моделью во время запуска агента.
     */
    List<ModelResponse> rawResponses;

    /**
     * Вывод последнего агента.
     */
    T finalOutput;

    AgentContext context;

    public List<ResponseInputItem> makeInputList() {
        var inputItemStream = newItems.stream()
                .map(RunItem::makeInputItem);
        return Stream.concat(input.stream(), inputItemStream).toList();
    }
}
