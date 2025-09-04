package ru.nyrk.agents.item.input;

import lombok.Builder;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.item.Role;

/**
 * Простой текстовый запрос для LLM
 */
@Builder
public record ResponseReasoningItemParam(String content, Role role) implements ResponseInputItem {

}