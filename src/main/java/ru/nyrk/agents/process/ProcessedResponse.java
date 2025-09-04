package ru.nyrk.agents.process;

import lombok.Builder;
import lombok.Value;
import ru.nyrk.agents.RunItem;

import java.util.List;

/**
 * Описывает действие которое необходимо проделать с ответом LLM
 */
@Value
@Builder
public class ProcessedResponse {

    /**
     * Общий список действий
     */
    @Builder.Default
    List<RunItem<?>> newItems = List.of();

    /**
     * Обработать инструменты
     */
    @Builder.Default
    List<String> toolsUsed = List.of();

    /**
     * Обработать преходы на суб агентов
     */
    @Builder.Default
    List<ToolRunHandoff> handoffs = List.of();

    /**
     * Вызвать инструменты
     */
    @Builder.Default
    List<ToolRunFunction> functions = List.of();

    public boolean hasToolsOrApprovalsToRun() {
        return !getHandoffs().isEmpty()
                || !getFunctions().isEmpty();
    }

}
