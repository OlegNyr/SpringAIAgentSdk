package ru.nyrk.agents.process;

import lombok.NonNull;
import org.springframework.ai.tool.ToolCallback;
import ru.nyrk.agents.*;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.RunItem;
import ru.nyrk.agents.ModelResponse;
import ru.nyrk.agents.runner.AgentOutputSchemaBase;
import ru.nyrk.agents.runner.SingleStepResult;

import java.util.List;

public interface AgentWorkerResponse {

    SingleStepResult getSingleStepResultFromResponse(Agent agent,
                                                     List<ResponseInputItem> originalInput,
                                                     List<RunItem<?>> preStepItems,
                                                     ModelResponse newResponse,
                                                     @NonNull AgentOutputSchemaBase<?> outputSchema,
                                                     List<ToolCallback> tools,
                                                     List<Handoff> handoffs,
                                                     AgentHooks agentHooks,
                                                     AgentContext agentContext,
                                                     RunConfig runConfig,
                                                     AgentToolUseTracker toolUseTracker);
}
