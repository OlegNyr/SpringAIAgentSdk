package ru.nyrk.agents;

import org.springframework.ai.tool.ToolCallback;
import ru.nyrk.agents.models.ResponseFunctionToolCall;

/**
 * События жизненного цикла (хуки)
 */
public interface AgentHooks {
    AgentHooks NONE = new AgentHooks() {
    };

    default void onAgentStart(AgentContext agentContext, Agent agent) {

    }

    default void onStart(AgentContext agentContext, Agent agent) {

    }

    default void onAgentEnd(AgentContext agentContext, Agent agent, Object finalOutput) {

    }

    default void onEnd(AgentContext agentContext, Agent agent, Object finalOutput) {
    }

    default void onToolStart(AgentContext agentContext, Agent agent,
                             ResponseFunctionToolCall responseFunctionToolCall,
                             ToolCallback toolCallback) {
    }

    default void onToolEnd(AgentContext agentContext, Agent agent,
                   ResponseFunctionToolCall responseFunctionToolCall,
                   ToolCallback toolCallback){}
}
