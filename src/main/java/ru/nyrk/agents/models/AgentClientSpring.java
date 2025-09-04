package ru.nyrk.agents.models;

import lombok.NonNull;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;
import ru.nyrk.agents.*;
import ru.nyrk.agents.runner.AgentOutputSchemaBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AgentClientSpring {
    private final RunConfig runConfig;

    public AgentClientSpring(RunConfig runConfig) {
        this.runConfig = runConfig;
    }

    /**
     * Get a response from the model.
     *
     * @param agent        Агент
     * @param systemPrompt The system instructions to use.
     * @param input        The input items to the model, in OpenAI Responses format.
     * @param outputSchema The output schema to use.
     * @param tools        The tools available to the model.
     * @param handoffs     The handoffs available to the model.
     * @param agentContext Контекст
     * @return The full model response.
     */
    public ModelResponse call(Agent agent,
                              String systemPrompt,
                              List<ResponseInputItem> input,
                              @NonNull AgentOutputSchemaBase<?> outputSchema,
                              List<ToolCallback> tools,
                              List<Handoff> handoffs,
                              AgentContext agentContext) {
        List<Message> messages = Converter.itemsToMessages(input);
        if (StringUtils.hasText(systemPrompt)) {
            messages.addFirst(new SystemMessage(systemPrompt));
        }
        ChatModel localChatModel = Objects.requireNonNullElse(runConfig.getModel(), agent.getModel());

        List<ToolCallback> allTools = new ArrayList<>(tools);
        handoffs.forEach(handoff -> allTools.add(Converter.toolHandoffTool(handoff)));

        ChatOptions chatOptions = agent.getChatOptions() == null
                ? localChatModel.getDefaultOptions() : agent.getChatOptions().copy();

        if (runConfig.getChatOptions() != null) {
            chatOptions = ModelOptionsUtils.merge(runConfig.getChatOptions(), chatOptions, chatOptions.getClass());
        }
        if (!allTools.isEmpty()) {
            if (chatOptions instanceof ToolCallingChatOptions toolCallingChatOptions) {
                allTools.addAll(toolCallingChatOptions.getToolCallbacks());
                toolCallingChatOptions.setToolCallbacks(allTools);
                toolCallingChatOptions.setInternalToolExecutionEnabled(false);
            } else {
                throw new RuntimeException();
            }
        }
        Prompt prompt = Prompt.builder()
                .chatOptions(chatOptions)
                .messages(messages)
                .build();

        if (!outputSchema.isPlanText()) {
            prompt = prompt
                    .augmentUserMessage(userMessage -> userMessage.mutate()
                            .text(userMessage.getText() + System.lineSeparator() + outputSchema.getFormat())
                            .build());
        }


        ChatResponse response = localChatModel.call(prompt);

        List<ResponseOutputItem> items = Converter.messageToOutputItems(response.getResult());

        Usage usage = Optional.of(response.getMetadata())
                .map(ChatResponseMetadata::getUsage)
                .orElseGet(EmptyUsage::new);
        return new ModelResponse(items, usage);
    }

}
