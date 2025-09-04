package ru.nyrk.agents.item;

import ru.nyrk.agents.Agent;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.item.input.ResponseOutputMessageParam;
import ru.nyrk.agents.models.ResponseFunctionToolCall;

/**
 * Ран который говорит о намерениях вызвать функцию
 */
public class ToolCallItem extends AbstractRunItem<ResponseFunctionToolCall> {

    public ToolCallItem(Agent agent, ResponseFunctionToolCall rawItem) {
        super(agent, rawItem);
    }

    @Override
    public ResponseInputItem makeInputItem() {
        return ResponseOutputMessageParam.builder()
                .arguments(rawItem.getArguments())
                .callId(rawItem.getCallId())
                .name(rawItem.getName())
                .build();
    }
}
