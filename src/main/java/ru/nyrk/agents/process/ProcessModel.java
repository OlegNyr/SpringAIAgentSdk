package ru.nyrk.agents.process;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import ru.nyrk.agents.*;
import ru.nyrk.agents.item.HandoffCallItem;
import ru.nyrk.agents.item.MessageOutputItem;
import ru.nyrk.agents.item.ReasoningItem;
import ru.nyrk.agents.item.ToolCallItem;
import ru.nyrk.agents.models.ResponseFunctionToolCall;
import ru.nyrk.agents.models.ResponseOutputMessage;
import ru.nyrk.agents.models.ResponseReasoningItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

@Slf4j(topic = "file")
public class ProcessModel {

    /// Конвертирует сырой ответ от модели в сущьности описывающие возврат,
    ///
    /// @param agent         Агент
    /// @param tools         список инструментов агента
    /// @param modelResponse ответ модели
    /// @param handoffs      список суб агентов для перехода
    /// @return Возврат структурированного разбора необходимых действий с результатом обработки от LLM
    public ProcessedResponse processResponse(Agent agent,
                                             List<ToolCallback> tools,
                                             ModelResponse modelResponse,
                                             List<Handoff> handoffs) {
        List<String> toolsUsed = new ArrayList<>();


        List<ToolRunFunction> functions = new ArrayList<>();
        Map<String, ToolCallback> functionMap = tools.stream()
                .collect(Collectors.toMap(t -> t.getToolDefinition().name(), Function.identity()));


        Map<String, Handoff> handoffMap = handoffs.stream()
                .collect(Collectors.toMap(Handoff::getToolName, Function.identity()));


        List<RunItem<?>> runItems = new ArrayList<>();
        List<ToolRunHandoff> runHandoffs = new ArrayList<>();
        for (ResponseOutputItem item : modelResponse.output()) {
            switch (item) {
                case ResponseOutputMessage message -> runItems.add(new MessageOutputItem(agent, message));
                case ResponseReasoningItem message -> runItems.add(new ReasoningItem(agent, message));
                case ResponseFunctionToolCall toolCall -> {
                    //LLM нам говорит что нужен вызов функции
                    toolsUsed.add(toolCall.getName());

                    if (handoffMap.containsKey(toolCall.getName())) {
                        //Функция переход на суб агента
                        runItems.add(new HandoffCallItem(agent, toolCall));
                        runHandoffs.add(new ToolRunHandoff(toolCall, handoffMap.get(toolCall.getName())));
                    } else {
                        //Нужно вызвать функцию
                        ToolCallback toolCallback = functionMap.get(toolCall.getName());
                        if (toolCallback == null) {
                            throw new ModelBehaviorError("Tool \"%s\" not found in agent \"%s\""
                                    .formatted(toolCall.getName(), agent.getName()));
                        }
                        runItems.add(new ToolCallItem(agent, toolCall));
                        functions.add(new ToolRunFunction(toolCall, toolCallback));
                    }
                }
                case null, default -> throw new UnsupportedOperationException();
            }
        }
        return ProcessedResponse.builder()
                .newItems(unmodifiableList(runItems))
                .toolsUsed(unmodifiableList(toolsUsed))
                .functions(unmodifiableList(functions))
                .handoffs(unmodifiableList(runHandoffs))
                .build();
    }
}
