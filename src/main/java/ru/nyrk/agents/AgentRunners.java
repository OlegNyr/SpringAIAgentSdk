package ru.nyrk.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.model.ModelOptionsUtils;
import ru.nyrk.agents.item.Role;
import ru.nyrk.agents.item.input.EasyInputMessageParam;
import ru.nyrk.agents.process.HandoffInputData;
import ru.nyrk.agents.runner.DefaultAgentRunner;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Builder(toBuilder = true, builderMethodName = "runner", buildMethodName = "create", builderClassName = "AgentRunnersClient")
@Value
public class AgentRunners {
    /**
     * Модель для использования во всем запуске агента. Если установлена, переопределяет модель,
     * установленную для каждого агента.
     */
    ChatModel model;

    /**
     * Настройка глобальных параметров модели. Любые ненулевые значения переопределяют
     * специфичные для агента настройки модели.
     */
    ChatOptions chatOptions;


    Double temperature;
    @Builder.Default
    Integer maxTokens = 1000;
    String modelName;
    Integer topK;
    Double topP;

    /**
     * Глобальный фильтр входных данных для применения ко всем передачам управления. Если
     * установлен `Handoff.inputFilter`, он будет иметь приоритет. Фильтр входных данных позволяет
     * редактировать входные данные, отправляемые новому агенту.
     */
    Function<HandoffInputData, HandoffInputData> handoffInputFilter;

    @Builder.Default
    AgentContext agentContext = new AgentContextDefault();

    @Builder.Default
    Integer maxTurns = 10;

    @Builder.Default
    AgentHooks agentHooks = AgentHooks.NONE;

    public <T> RunResult<T> run(Agent startingAgent, Collection<ResponseInputItem> input) {
        DefaultAgentRunner<T> agentRunner = new DefaultAgentRunner<>();
        ChatOptions chatOptionsLocal = chatOptions;
        if (model == null && startingAgent.getModel() == null) {
            throw new IllegalArgumentException("Model not setting");
        }

        if (temperature != null || maxTokens != null || modelName != null || topK != null || topP != null) {
            DefaultChatOptions mutate = new JsonDefaultChatOptions();
            mutate.setTemperature(temperature);
            mutate.setMaxTokens(maxTokens);
            mutate.setModel(modelName);
            mutate.setTopK(topK);
            mutate.setTopP(topP);
            if (chatOptions == null) {
                chatOptionsLocal = mutate;
            } else {
                chatOptionsLocal = ModelOptionsUtils.merge(mutate, chatOptions, chatOptions.getClass());
            }
        }
        RunConfig runConfig = RunConfig.builder()
                .chatOptions(chatOptionsLocal)
                .model(model)
                .build();
        return agentRunner.run(startingAgent, List.copyOf(input), agentContext, maxTurns, agentHooks, runConfig);
    }

    public AgentRunnersClient copy() {
        return this.toBuilder();
    }


    public static class AgentRunnersClient {

        public AgentRunnersClient filterTool() {
            this.handoffInputFilter(InputFiltersUtils::removeAllTools);
            return this;
        }

        public AgentRunnersClient copy() {
            return this.create().copy();
        }

        public <T> RunResult<T> run(Agent startingAgent, String input) {
            return this.create().run(startingAgent, List.of(new EasyInputMessageParam(input, Role.USER)));
        }

        public <T> RunResult<T> run(Agent startingAgent, ResponseInputItem input) {
            return this.create().run(startingAgent, List.of(input));
        }

        public <T> RunResult<T> run(Agent startingAgent, Collection<ResponseInputItem> input) {
            return this.create().run(startingAgent, input);
        }
    }

    public static class JsonDefaultChatOptions extends DefaultChatOptions {

        @JsonProperty("max_tokens")
        @Override
        public Integer getMaxTokens() {
            return super.getMaxTokens();
        }

        @JsonProperty("top_p")
        @Override
        public Double getTopP() {
            return super.getTopP();
        }

        @JsonProperty("top_k")
        @Override
        public Integer getTopK() {
            return super.getTopK();
        }
    }
}
