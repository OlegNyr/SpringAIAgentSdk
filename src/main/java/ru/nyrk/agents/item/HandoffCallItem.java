package ru.nyrk.agents.item;

import lombok.ToString;
import ru.nyrk.agents.Agent;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.item.input.ResponseOutputMessageParam;
import ru.nyrk.agents.models.ResponseFunctionToolCall;

/**
 * Ран описывает что нужен переход от одного агента к другому
 */
@ToString(callSuper = true)
public class HandoffCallItem extends AbstractRunItem<ResponseFunctionToolCall> {

    public HandoffCallItem(Agent agent, ResponseFunctionToolCall rawItem) {
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
