package ru.nyrk.agents;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class AgentToolUseTracker {

    @Getter
    private final List<ToolUse> agentToolUse = new ArrayList<>();

    public void addToolUse(Agent agent, List<String> toolsUsed) {
        agentToolUse.add(new ToolUse(agent, toolsUsed));
    }

    record ToolUse(Agent agent, List<String> toolName) {

    }
}
