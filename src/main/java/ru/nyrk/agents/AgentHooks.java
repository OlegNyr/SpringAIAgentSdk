package ru.nyrk.agents;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.tool.ToolCallback;
import ru.nyrk.agents.models.ResponseFunctionToolCall;

/**
 * События жизненного цикла (хуки)
 */
public interface AgentHooks extends CallAdvisor {
    AgentHooks NONE = new AgentHooks() {
        @Override
        public int getOrder() {
            return Integer.MAX_VALUE;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
            return callAdvisorChain.nextCall(chatClientRequest);
        }

        @Override
        public String getName() {
            return "NONE";
        }
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
                           ToolCallback toolCallback) {
    }
}
