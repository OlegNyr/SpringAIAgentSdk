package ru.nyrk.agents.process;

import org.springframework.ai.chat.model.ToolContext;
import ru.nyrk.agents.*;
import ru.nyrk.agents.item.input.FunctionCallOutput;
import ru.nyrk.agents.item.ToolCallResultItem;
import ru.nyrk.agents.runner.AgentsException;

import java.util.ArrayList;
import java.util.List;

public class AgentToolCallingManager {

    public List<FunctionToolResult> executeFunction(Agent agent,
                                                    List<ToolRunFunction> functions,
                                                    AgentHooks agentHooks,
                                                    AgentContext agentContext,
                                                    RunConfig runConfig) {
        List<FunctionToolResult> tasks = new ArrayList<>();
        for (ToolRunFunction function : functions) {
            try {
                agentHooks.onToolStart(agentContext, agent, function.toolCall(), function.toolCallback());
                ToolContext toolContext = agentContext.makeToolContext();

                String result = function.toolCallback().call(function.toolCall().getArguments(), toolContext);

                FunctionCallOutput functionCallOutput = ItemHelpers.toolCallOutputItem(function.toolCall(), result);

                tasks.add(FunctionToolResult.builder()
                        .tool(function.toolCallback())
                        .result(result)
                        .runItem(new ToolCallResultItem(agent, functionCallOutput, result))
                        .build());

                agentHooks.onToolEnd(agentContext, agent, function.toolCall(), function.toolCallback());
            } catch (AgentsException ea) {
                throw ea;
            } catch (Exception e) {
                throw new UserErrorException("Error running tool %s:".formatted(function.toolCall().getName()), e);
            }
        }
        return tasks;
    }


}
