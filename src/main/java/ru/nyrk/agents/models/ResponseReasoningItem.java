package ru.nyrk.agents.models;

import lombok.Value;
import ru.nyrk.agents.ResponseOutputItem;
import ru.nyrk.agents.item.Role;

/**
 * Возвращается моделью контент
 */
@Value(staticConstructor = "of")
public class ResponseReasoningItem implements ResponseOutputItem {

    /**
     * Reasoning summary content
     */
    String summary;

    /**
     * The role of the output message
     */
    Role role;
}
