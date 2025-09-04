package ru.nyrk.agents.item;

import lombok.ToString;
import ru.nyrk.agents.Agent;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.item.input.FunctionCallOutput;

/**
 * Результат работы функции агента, result результат работы функции, в формате json
 */
@ToString(callSuper = true)
public class ToolCallResultItem extends AbstractRunItem<FunctionCallOutput> {

    private final String result;

    public ToolCallResultItem(Agent agent, FunctionCallOutput rawItem, String result) {
        super(agent, rawItem);
        this.result = result;
    }

    @Override
    public ResponseInputItem makeInputItem() {
        return rawItem;
    }
}
