package ru.nyrk.agents;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

public interface AgentContext extends Map<String, Object> {
    void addUsage(Usage usage);

    ToolContext makeToolContext();
}
