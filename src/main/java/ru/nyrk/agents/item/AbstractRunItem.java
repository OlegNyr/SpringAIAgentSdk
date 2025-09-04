package ru.nyrk.agents.item;


import lombok.Value;
import lombok.experimental.NonFinal;
import ru.nyrk.agents.Agent;
import ru.nyrk.agents.RunItem;

/**
 * @see RunItem
 */
@NonFinal
@Value
public abstract class AbstractRunItem<T> implements RunItem<T> {

    Agent agent;

    protected T rawItem;

    public AbstractRunItem(Agent agent, T rawItem) {
        this.agent = agent;
        this.rawItem = rawItem;
    }
}
