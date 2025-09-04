package ru.nyrk.agents;

import lombok.experimental.Delegate;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ToolContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentContextDefault implements AgentContext {
    @Delegate
    Map<String, Object> data = new LinkedHashMap<>();
    Usage usage = new EmptyUsage();

    @Override
    public void addUsage(Usage usage) {
        List<Object> nativeObject;
        if (this.usage.getNativeUsage() instanceof List<?> list) {
            nativeObject = (List<Object>) list;
        }
        else {
            nativeObject = new ArrayList<>();
        }
        nativeObject.add(usage.getNativeUsage());
        this.usage = new DefaultUsage(usage.getPromptTokens() + this.usage.getPromptTokens(),
                usage.getCompletionTokens() + this.usage.getCompletionTokens(),
                usage.getTotalTokens() + this.usage.getTotalTokens(),
                nativeObject
        );
    }

    @Override
    public ToolContext makeToolContext() {
        return new ToolContext(Map.copyOf(this));
    }
}
