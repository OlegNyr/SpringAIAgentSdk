package ru.nyrk.agents.item;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.nyrk.agents.Agent;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.item.input.FunctionCallOutput;

/**
 * Ран говорит что необходимо перевести от одного агента к другому
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HandoffOutputItem extends AbstractRunItem<FunctionCallOutput> {

    Agent sourceAgent;
    Agent targetAgent;

    public HandoffOutputItem(Agent agent, FunctionCallOutput rawItem, Agent sourceAgent, Agent targetAgent) {
        super(agent, rawItem);
        this.sourceAgent = sourceAgent;
        this.targetAgent = targetAgent;
    }

    @Override
    public ResponseInputItem makeInputItem() {
        return FunctionCallOutput.builder()
                .callId(rawItem.getCallId())
                .result(rawItem.getResult())
                .name(rawItem.getName())
                .build();
    }
}
