package ru.nyrk.agents.item;

import lombok.ToString;
import ru.nyrk.agents.Agent;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.item.input.EasyInputMessageParam;
import ru.nyrk.agents.models.ResponseOutputMessage;

/**
 * Текстовое сообщение которое возвращает модель
 */
@ToString(callSuper = true)
public class MessageOutputItem extends AbstractRunItem<ResponseOutputMessage> {

    public MessageOutputItem(Agent agent, ResponseOutputMessage rawItem) {
        super(agent, rawItem);
    }

    @Override
    public ResponseInputItem makeInputItem() {
        return EasyInputMessageParam.builder()
                .role(Role.ASSISTANT)
                .content(rawItem.getContent())
                .build();
    }
}
