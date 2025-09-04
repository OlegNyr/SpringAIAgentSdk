package ru.nyrk.agents.models;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.StringUtils;
import ru.nyrk.agents.Handoff;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.ResponseOutputItem;
import ru.nyrk.agents.item.*;
import ru.nyrk.agents.item.input.EasyInputMessageParam;
import ru.nyrk.agents.item.input.FunctionCallOutput;
import ru.nyrk.agents.item.input.ResponseOutputMessageParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class Converter {
    public static final String FAKE_RESPONSES_ID = "__fake_id__";

    /**
     * Convert a sequence of 'Item' objects into a list of ChatCompletionMessageParam.
     * <p>
     * Rules:
     * - EasyInputMessage or InputMessage (role=user) => ChatCompletionUserMessageParam
     * - EasyInputMessage or InputMessage (role=system) => ChatCompletionSystemMessageParam
     * - EasyInputMessage or InputMessage (role=developer) => ChatCompletionDeveloperMessageParam
     * - InputMessage (role=assistant) => Start or flush a ChatCompletionAssistantMessageParam
     * - response_output_message => Also produces/flushes a ChatCompletionAssistantMessageParam
     * - tool calls get attached to the *current* assistant message, or create one if none.
     * - tool outputs => ChatCompletionToolMessageParam
     *
     * @param input
     * @return
     * @link D:\IdeaProjects\agents\mybackend\Lib\site-packages\agents\models\chatcmpl_converter.py#items_to_messages
     */
    public static List<Message> itemsToMessages(List<ResponseInputItem> input) {
        List<Message> messages = new ArrayList<Message>();
        for (ResponseInputItem item : input) {
            if (item instanceof EasyInputMessageParam e) {
                if (e.role() == Role.USER) {
                    messages.add(new UserMessage(e.content()));
                } else if (e.role() == Role.ASSISTANT) {
                    messages.add(new AssistantMessage(e.content()));
                } else {
                    throw new IllegalArgumentException("Ошибка роли");
                }
            } else if (item instanceof ResponseOutputMessageParam p) {
                messages.add(new AssistantMessage("", Map.of(),
                        List.of(new AssistantMessage.ToolCall(p.getCallId(), "function", p.getName(), p.getArguments())))
                );
            } else if (item instanceof FunctionCallOutput r) {
                messages.add(new ToolResponseMessage(List.of(new ToolResponseMessage.ToolResponse(r.getCallId(), r.getName(), r.getResult()))));
            } else {
                throw new IllegalArgumentException("Ошибка типа");
            }
        }
        return messages;
    }

    public static List<ResponseOutputItem> messageToOutputItems(Generation result) {
        if (result == null) {
            return List.of();
        }
        List<ResponseOutputItem> items = new ArrayList<>();
        AssistantMessage output = result.getOutput();
        if (StringUtils.hasText(output.getText())) {
            items.add(ResponseOutputMessage.of(output.getText(), Role.ASSISTANT));
        }
        if (output.hasToolCalls()) {
            for (AssistantMessage.ToolCall toolCall : output.getToolCalls()) {
                if ("function".equals(toolCall.type())) {
                    items.add(ResponseFunctionToolCall.builder()
                            .callId(toolCall.id())
                            .arguments(toolCall.arguments())
                            .name(toolCall.name())
                            .build());
                } else if ("custom".equals(toolCall.type())) {
                    log.warn("function \"{}\" custom  not supported ", toolCall.name());
                } else {
                    log.warn("function \"{}\" type {} not supported ",
                            toolCall.name(), toolCall.type());
                }
            }
        }
        return items;
    }

    public static ToolCallback toolHandoffTool(Handoff handoff) {
        return new HandoffToolCallback(
                DefaultToolDefinition.builder()
                        .name(handoff.getToolName())
                        .description(handoff.getToolDescription())
                        .inputSchema(handoff.getInputJsonSchema())
                        .build()
        );
    }

    @Value
    private static class HandoffToolCallback implements ToolCallback {
        ToolDefinition toolDefinition;

        @Override
        public String call(String toolInput) {
            throw new UnsupportedOperationException();
        }
    }
}
