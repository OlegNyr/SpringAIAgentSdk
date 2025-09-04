package ru.nyrk.agents.process;

import lombok.extern.slf4j.Slf4j;
import ru.nyrk.agents.*;
import ru.nyrk.agents.item.HandoffOutputItem;
import ru.nyrk.agents.item.ToolCallResultItem;
import ru.nyrk.agents.item.input.FunctionCallOutput;
import ru.nyrk.agents.runner.NextStepHandoff;
import ru.nyrk.agents.runner.SingleStepResult;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HandoffManager {

    public SingleStepResult execHop(Agent agent,
                                    List<ResponseInputItem> originalInput,
                                    List<RunItem<?>> preStepItems,
                                    List<RunItem<?>> newStepItems,
                                    ModelResponse newResponse,
                                    List<ToolRunHandoff> runHandoffs,
                                    AgentContext agentContext) {
        List<RunItem<?>> handoffStepItems = new ArrayList<>(newStepItems);
        boolean multipleHandoffs = runHandoffs.size() > 1;
        if (multipleHandoffs) {
            for (ToolRunHandoff runHandoff : runHandoffs) {
                String outputMessage = "Multiple handoffs detected, ignoring this one.";
                handoffStepItems.add(new ToolCallResultItem(agent,
                        ItemHelpers.toolCallOutputItem(runHandoff.toolCall(), outputMessage),
                        outputMessage
                ));
            }
        }
        ToolRunHandoff actualHandoff = runHandoffs.getFirst();
        Handoff handoff = actualHandoff.handoff();
        Agent newAgent = handoff.getOnInvokeHandoff().apply(agentContext, actualHandoff.toolCall().getArguments());
        FunctionCallOutput functionCallOutput = ItemHelpers.toolCallOutputItem(actualHandoff.toolCall(), handoff.getTransferMessage(newAgent));
        handoffStepItems.add(new HandoffOutputItem(agent, functionCallOutput, agent, newAgent));
        var inputFilter = handoff.getInputFilter();
        if (inputFilter != null) {
            log.debug("Filtering inputs for handoff");
            var handoffInputData = HandoffInputData.builder()
                    .inputHistory(List.copyOf(originalInput))
                    .preHandoffItems(preStepItems)
                    .newItems(newStepItems)
                    .runContext(agentContext)
                    .build();
            HandoffInputData filtered = inputFilter.apply(handoffInputData);

            if (filtered == null) {
                throw new UserErrorException("Invalid input filter result null");
            }
            originalInput = filtered.getInputHistory();
            preStepItems = filtered.getPreHandoffItems();
            handoffStepItems = filtered.getNewItems();
        }
        return SingleStepResult.builder()
                .originalInput(originalInput)
                .modelResponse(newResponse)
                .preStepItems(preStepItems)
                .newStepItems(handoffStepItems)
                .nextStep(new NextStepHandoff(newAgent))
                .build();
    }

}
