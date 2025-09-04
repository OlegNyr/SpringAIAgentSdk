package ru.nyrk.agents.process;

import lombok.NonNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;
import ru.nyrk.agents.*;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.ModelResponse;
import ru.nyrk.agents.item.AbstractRunItem;
import ru.nyrk.agents.item.MessageOutputItem;
import ru.nyrk.agents.models.ResponseOutputMessage;
import ru.nyrk.agents.runner.AgentOutputSchemaBase;
import ru.nyrk.agents.runner.NextStepFinalOutput;
import ru.nyrk.agents.runner.NextStepRunAgain;
import ru.nyrk.agents.runner.SingleStepResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DefaultAgentWorkerResponse implements AgentWorkerResponse {

    private final ProcessModel processModel = new ProcessModel();
    private final AgentToolCallingManager toolCallingManager = new AgentToolCallingManager();
    private final HandoffManager handoffManager = new HandoffManager();

    @Override
    public SingleStepResult getSingleStepResultFromResponse(Agent agent,
                                                            List<ResponseInputItem> originalInput,
                                                            List<RunItem<?>> preStepItems,
                                                            ModelResponse newResponse,
                                                            @NonNull AgentOutputSchemaBase<?> outputSchema,
                                                            List<ToolCallback> tools,
                                                            List<Handoff> handoffs,
                                                            AgentHooks agentHooks,
                                                            AgentContext agentContext,
                                                            RunConfig runConfig,
                                                            AgentToolUseTracker toolUseTracker) {
        ProcessedResponse processedResponse = processModel.processResponse(
                agent,
                tools,
                newResponse,
                handoffs
        );

        toolUseTracker.addToolUse(agent, processedResponse.getToolsUsed());

        preStepItems = new ArrayList<>(preStepItems);

        List<RunItem<?>> newStepItems = new ArrayList<>(processedResponse.getNewItems());

        List<FunctionToolResult> functionResults = List.of();
        if (!processedResponse.getFunctions().isEmpty()) {
            //Вызов функций
            functionResults = toolCallingManager
                    .executeFunction(agent,
                            processedResponse.getFunctions(),
                            agentHooks,
                            agentContext,
                            runConfig);
            for (var functionResult : functionResults) {
                newStepItems.add(functionResult.getRunItem());
            }
        }

        if (!processedResponse.getHandoffs().isEmpty()) {
            return handoffManager.execHop(agent,
                    originalInput,
                    preStepItems,
                    List.copyOf(newStepItems),
                    newResponse,
                    processedResponse.getHandoffs(),
                    agentContext
            );
        }

        ToolsToFinalOutputResult checkToolUse = checkForFinalOutputFromTools(functionResults);
        if (checkToolUse.isFinalOutput()) {
            return executeFinalOutput(agent,
                    originalInput,
                    newResponse,
                    preStepItems,
                    newStepItems,
                    checkToolUse.finalOutput(),
                    agentHooks,
                    agentContext);
        }

        //Возмем последний текст от LLM
        String potentialFinalOutputText = extractLastText(newStepItems);

        //Есть два варианта, которые приводят к конечному результату:
        //1. Структурированная схема вывода => всегда приводит к конечному результату
        //2. Схема вывода в виде простого текста => приводит к конечному результату при отсутствии вызовов инструментов
        if (!outputSchema.isPlanText() && StringUtils.hasText(potentialFinalOutputText)) {
            Object finalOutput = outputSchema.convert(potentialFinalOutputText);
            return executeFinalOutput(agent,
                    originalInput,
                    newResponse,
                    preStepItems,
                    newStepItems,
                    finalOutput,
                    agentHooks,
                    agentContext);
        } else if (outputSchema.isPlanText() && !processedResponse.hasToolsOrApprovalsToRun()) {
            return executeFinalOutput(agent,
                    originalInput,
                    newResponse,
                    preStepItems,
                    newStepItems,
                    potentialFinalOutputText,
                    agentHooks,
                    agentContext);
        } else {
            return SingleStepResult.builder()
                    .originalInput(originalInput)
                    .modelResponse(newResponse)
                    .preStepItems(preStepItems)
                    .newStepItems(newStepItems)
                    .nextStep(new NextStepRunAgain())
                    .build();
        }
    }

    private SingleStepResult executeFinalOutput(Agent agent,
                                                List<ResponseInputItem> originalInput,
                                                ModelResponse newResponse,
                                                List<RunItem<?>> preStepItems,
                                                List<RunItem<?>> newStepItems,
                                                Object finalOutput,
                                                AgentHooks agentHooks,
                                                AgentContext agentContext) {
        agentHooks.onAgentEnd(agentContext, agent, finalOutput);
        agentHooks.onEnd(agentContext, agent, finalOutput);

        return SingleStepResult.builder()
                .originalInput(originalInput)
                .modelResponse(newResponse)
                .preStepItems(preStepItems)
                .newStepItems(newStepItems)
                .nextStep(new NextStepFinalOutput(finalOutput))
                .build();
    }


    private ToolsToFinalOutputResult checkForFinalOutputFromTools(List<FunctionToolResult> functionResults) {
        for (FunctionToolResult functionResult : functionResults) {
            if (functionResult.getTool().getToolMetadata().returnDirect()) {
                return new ToolsToFinalOutputResult(true, functionResult.getResult());
            }
        }
        return new ToolsToFinalOutputResult(false, null);
    }


    private record ToolsToFinalOutputResult(boolean isFinalOutput, String finalOutput) {
    }

    private static String extractLastText(List<RunItem<?>> newStepItems) {
        if (newStepItems.isEmpty()) {
            return null;
        }
        Optional<ResponseOutputMessage> last = newStepItems.stream()
                .filter(MessageOutputItem.class::isInstance)
                .map(MessageOutputItem.class::cast)
                .reduce((a, e) -> a)
                .map(AbstractRunItem::getRawItem);

        return last
                .map(ResponseOutputMessage::getContent)
                .orElse(null);

    }
}
