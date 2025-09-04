package ru.nyrk.agents.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import ru.nyrk.agents.*;
import ru.nyrk.agents.item.*;
import ru.nyrk.agents.item.input.EasyInputMessageParam;
import ru.nyrk.agents.models.AgentModel;
import ru.nyrk.agents.ModelResponse;
import ru.nyrk.agents.process.AgentWorkerResponse;
import ru.nyrk.agents.process.DefaultAgentWorkerResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * https://openai.github.io/openai-agents-python/running_agents/
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultAgentRunner implements AgentRunner {

    private final ChatModel chatModel;
    private final AgentWorkerResponse agentWorkerResponse = new DefaultAgentWorkerResponse();

    /// @see DefaultAgentRunner#run
    @Override
    public <T> RunResult<T> run(Agent startingAgent, Object user) {

        List<ResponseInputItem> input = switch (user) {
            case String txt -> List.of(new EasyInputMessageParam(txt, Role.USER));
            case ResponseInputItem inputCast -> List.of(inputCast);
            case List<?> inputList -> inputList.stream()
                    .filter(ResponseInputItem.class::isInstance)
                    .map(ResponseInputItem.class::cast)
                    .toList();
            case null, default -> throw new IllegalArgumentException("user is not a string");
        };
        return run(startingAgent,
                input,
                new AgentContextDefault(),
                10,
                AgentHooks.NONE,
                new RunConfig()
        );
    }

    /**
     * @inheritDoc
     */
    @Override
    public <T> RunResult<T> run(Agent startingAgent,
                                List<ResponseInputItem> input,
                                AgentContext agentContext,
                                Integer maxTurns,
                                AgentHooks agentHooks,
                                RunConfig runConfig) {
        AgentToolUseTracker toolUseTracker = new AgentToolUseTracker();
        int currentTurn = 0;
        List<ResponseInputItem> originalInput = List.copyOf(input);
        List<RunItem<?>> generatedItems = List.of();
        Agent currentAgent = startingAgent;

        List<ModelResponse> modelResponses = new ArrayList<>();
        boolean shouldRunAgentStartHooks = true;

        while (true) {
            currentTurn++;
            if (currentTurn > maxTurns) {
                throw new MaxTurnsExceeded("Max turns (%d) exceeded".formatted(maxTurns));
            }

            log.debug("Running agent {} (turn {})", currentAgent.getName(), currentTurn);

            //вызов агента
            SingleStepResult turnResult = runSingleTurn(
                    currentAgent,
                    currentAgent.getTools(),
                    originalInput,
                    generatedItems,
                    agentHooks,
                    agentContext,
                    runConfig,
                    shouldRunAgentStartHooks,
                    toolUseTracker
            );

            shouldRunAgentStartHooks = false;

            modelResponses.add(turnResult.getModelResponse());
            originalInput = turnResult.getOriginalInput();
            generatedItems = turnResult.unionGeneratedItems();


            if (turnResult.getNextStep() instanceof NextStepFinalOutput stepFinalOutput) {
                //Финальный статус
                //todo разобраться с дженереками
                return (RunResult<T>) RunResult.builder()
                        .input(originalInput)
                        .newItems(generatedItems)
                        .rawResponses(modelResponses)
                        .finalOutput(stepFinalOutput.output())
                        .lastAgent(currentAgent)
                        .context(agentContext)
                        .build();
            } else if (turnResult.getNextStep() instanceof NextStepHandoff stepHandoff) {
                currentAgent = stepHandoff.newAgent();
                shouldRunAgentStartHooks = true;
                log.debug("Передаем управление агенту \"{}\"", currentAgent.getName());
            } else if (turnResult.getNextStep() instanceof NextStepRunAgain) {
                log.debug("Следующая итерация");
            } else {
                throw new AgentsException("Unknown next step type: %s".formatted(turnResult.getNextStep().getClass()));
            }
        }

    }

    private SingleStepResult runSingleTurn(Agent agent,
                                           List<ToolCallback> tools,
                                           List<ResponseInputItem> originalInput,
                                           List<RunItem<?>> generatedItems,
                                           AgentHooks agentHooks,
                                           AgentContext agentContext,
                                           RunConfig runConfig,
                                           boolean shouldRunAgentStartHooks,
                                           AgentToolUseTracker toolUseTracker) {
        // Ensure we run the hooks before anything else
        if (shouldRunAgentStartHooks) {
            agentHooks.onAgentStart(agentContext, agent);
            agent.getHooks().onStart(agentContext, agent);
        }

        String systemPrompt = agent.getSystemPrompt(agentContext);

        AgentOutputSchemaBase<?> outputSchema = new AgentOutputSchemaBase<>(agent.getOutputType());

        List<Handoff> handoffs = getHandoffs(agent, agentContext);
        //Преобразуем входящие элементы в понятный формат
        List<ResponseInputItem> input = new ArrayList<>(originalInput);
        //Преобразуем входящие действия в формат который можно передать LLM
        generatedItems.forEach(g -> input.add(g.makeInputItem()));

        AgentModel agentModel = new AgentModel(chatModel);

        ModelResponse newResponse = agentModel.call(
                agent,
                systemPrompt,
                input,
                outputSchema,
                tools,
                handoffs,
                agentContext,
                runConfig
        );

        agentContext.addUsage(newResponse.usage());

        return agentWorkerResponse.getSingleStepResultFromResponse(
                agent,
                originalInput,
                generatedItems,
                newResponse,
                outputSchema,
                tools,
                handoffs,
                agentHooks,
                agentContext,
                runConfig,
                toolUseTracker
        );
    }

    private List<Handoff> getHandoffs(Agent agent, AgentContext agentContext) {
        List<Handoff> handoffs = new ArrayList<>(agent.getHandoffs());
        for (Agent agentHandoff : agent.getHandoffAgents()) {
            handoffs.add(Handoff.makeHandoff(agentHandoff));
        }

        return handoffs.stream()
                .filter(h -> h.getEnabled().test(agentContext, agent))
                .toList();
    }


}
