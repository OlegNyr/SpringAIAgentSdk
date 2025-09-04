package ru.nyrk.agents.models;

import lombok.Value;
import ru.nyrk.agents.ResponseOutputItem;
import ru.nyrk.agents.item.Role;

/**
 * Возвращается моделью контент
 */
@Value(staticConstructor = "of")
public class ResponseOutputMessage implements ResponseOutputItem {

    /**
     * The content of the output message.
     */
    String content;

    /**
     * The role of the output message
     */
    Role role;
}
