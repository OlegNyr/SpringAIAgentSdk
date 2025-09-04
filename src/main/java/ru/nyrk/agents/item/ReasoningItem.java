package ru.nyrk.agents.item;

import lombok.ToString;
import ru.nyrk.agents.Agent;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.item.input.ResponseReasoningItemParam;
import ru.nyrk.agents.models.ResponseReasoningItem;

/**
 * Текстовое сообщение которое возвращает модель
 */
@ToString(callSuper = true)
public class ReasoningItem extends AbstractRunItem<ResponseReasoningItem> {

    public ReasoningItem(Agent agent, ResponseReasoningItem rawItem) {
        super(agent, rawItem);
    }

    @Override
    public ResponseInputItem makeInputItem() {
        return ResponseReasoningItemParam.builder()
                .role(Role.ASSISTANT)
                .content(rawItem.getSummary())
                .build();
    }
}
