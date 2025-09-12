package ru.nyrk.agents;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.DefaultToolMetadata;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.core.ParameterizedTypeReference;
import ru.nyrk.agents.item.MessageOutputItem;
import ru.nyrk.agents.item.Role;
import ru.nyrk.agents.item.input.EasyInputMessageParam;
import ru.nyrk.agents.models.ResponseOutputMessage;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * агент взят <a href="https://openai.github.io/openai-agents-python/agents/">отсюда</a>
 */
@Slf4j
@Value
@Builder(toBuilder = true, builderClassName = "AgentBuilder")
@EqualsAndHashCode(exclude = {"handoffs", "handoffAgents"})
@ToString(exclude = {"handoffs", "handoffAgents"})
public class Agent {

    public static final ToolMetadata TOOL_METADATA_EMPTY = DefaultToolMetadata.builder().build();
    /**
     * Обязательная строка, идентифицирующая вашего агента.
     */
    @NonNull
    String name;

    /// Описание используемое для перехода на данного агента
    String handoffDescription;

    /// **SystemPrompt** - также известно как сообщение для разработчиков или системное уведомление.
    String instructions;

    BiFunction<Agent, AgentContext, String> instructionFunction;

    /// **Вспомогательные агенты** - это субагенты, которым агент может делегировать полномочия.
    ///
    /// Вы предоставляете список вспомогательных агентов, и агент может делегировать им полномочия, если это необходимо.
    ///
    /// Это эффективный шаблон, который позволяет управлять модульными  специализированными агентами,
    /// выполняющими одну задачу
    @Singular("agent")
    List<Agent> handoffAgents;

    @Singular("handoff")
    List<Handoff> handoffs;


    /// какую LLM использовать, и необязательный `chatOptions`
    /// для настройки параметров модели, таких как `temperature`, `top_p` и т. д.
    ChatModel model;

    ChatOptions chatOptions;

    @Singular("tool")
    List<ToolCallback> tools;


    /**
     * Типы выходных данных
     */
    StructuredOutputConverter<?> outputType;

    @Singular("advisor")
    List<Advisor> advisors;

    @NonNull
    @Builder.Default
    AgentHooks hooks = AgentHooks.NONE;

    public String getSystemPrompt(AgentContext agentContext) {
        if (instructions == null) {
            log.error("Instructions must be a string or a function");
        }
        return instructions;
    }

    /// Агент как функция
    ///
    /// Это отличается от передачи обслуживания двумя способами:
    ///
    /// 1. При передаче обслуживания новый агент получает историю разговора. В этом инструменте новый агент
    /// получает сгенерированные входные данные.
    ///
    /// 2. При передаче обслуживания новый агент принимает управление разговором. В этом инструменте новый агент
    /// вызывается как инструмент, и разговор продолжается исходным агентом.
    public ToolCallback makeTool(AgentRunners agentRunner, String toolName, String toolDescription) {
        Function<ToolAgentParam, String> function = (ToolAgentParam param) -> {
            var res = agentRunner.run(this, List.of(new EasyInputMessageParam(param.input(), Role.USER)));
            return joinResponse(res);
        };

        return FunctionToolCallback.builder(toolName, function)
                .description(toolDescription)
                .inputType(ToolAgentParam.class)
                .inputSchema(JsonSchemaGenerator.generateForType(ToolAgentParam.class))
                .inputType(ParameterizedTypeReference.forType(ToolAgentParam.class))
                .toolMetadata(TOOL_METADATA_EMPTY)
                .toolCallResultConverter(new DefaultToolCallResultConverter())
                .build();
    }

    private record ToolAgentParam(String input) {
    }

    public static class AgentBuilder {

        public AgentBuilder outputType(final ParameterizedTypeReference<?> type) {
            this.outputType = new BeanOutputConverter<>(type);
            return this;
        }

        public AgentBuilder outputType(final StructuredOutputConverter<?> type) {
            this.outputType = type;
            return this;
        }

        public AgentBuilder outputType(final Class<?> type) {
            this.outputType = new BeanOutputConverter<>(type);
            return this;
        }

        public <I, O> BuilderToolFunction<I, O> toolFunction(Function<I, O> function, Class<I> inputType) {
            BiFunction<I, ToolContext, O> toolFunction = (request, context) -> function.apply(request);
            return new BuilderToolFunction<I, O>(this, toolFunction, inputType);
        }

    }

    public static class BuilderToolFunction<I, O> {
        private final AgentBuilder agentBuilder;
        private final BiFunction<I, ToolContext, O> toolFunction;
        private final Class<I> type;

        private String description;
        private String name;

        public BuilderToolFunction(AgentBuilder agentBuilder,
                                   BiFunction<I, ToolContext, O> toolFunction,
                                   Class<I> type) {
            this.agentBuilder = agentBuilder;
            this.toolFunction = toolFunction;
            this.type = type;
        }


        public BuilderToolFunction<I, O> description(String description) {
            this.description = description;
            return this;
        }

        public BuilderToolFunction<I, O> name(String name) {
            this.name = name;
            return this;
        }

        AgentBuilder and() {
            FunctionToolCallback<I, O> toolCallback = FunctionToolCallback.builder(name, toolFunction)
                    .description(description)
                    .inputType(type)
                    .inputSchema(JsonSchemaGenerator.generateForType(type))
                    .inputType(ParameterizedTypeReference.forType(type))
                    .toolMetadata(TOOL_METADATA_EMPTY)
                    .toolCallResultConverter(new DefaultToolCallResultConverter())
                    .build();

            agentBuilder.tool(toolCallback);
            return agentBuilder;
        }
    }

    private static String joinResponse(RunResult<Object> res) {
        StringBuilder sb = new StringBuilder();
        for (RunItem<?> newItem : res.getNewItems()) {
            if (newItem instanceof MessageOutputItem messageOutputItem) {
                if (messageOutputItem.getRawItem() instanceof ResponseOutputMessage m) {
                    if (m.getContent() != null) {
                        sb.append(m.getContent());
                    }
                }
            }
        }
        return sb.toString();
    }

}
