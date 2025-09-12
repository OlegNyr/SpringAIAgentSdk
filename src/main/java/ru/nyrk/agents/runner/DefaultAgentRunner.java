package ru.nyrk.agents.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import ru.nyrk.agents.*;
import ru.nyrk.agents.models.AgentClientSpring;
import ru.nyrk.agents.process.AgentWorkerResponse;
import ru.nyrk.agents.process.DefaultAgentWorkerResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * https://openai.github.io/openai-agents-python/running_agents/
 */
@Slf4j
public class DefaultAgentRunner<T> {

    private final AgentWorkerResponse agentWorkerResponse;

    public DefaultAgentRunner(AgentWorkerResponse agentWorkerResponse) {
        this.agentWorkerResponse = agentWorkerResponse;
    }

    public DefaultAgentRunner() {
        this.agentWorkerResponse = new DefaultAgentWorkerResponse();
    }

    /// Запускает рабочий процесс, начиная с указанного агента.
    /// Агент будет работать в цикле, пока не будет сгенерирован конечный выход.
    ///
    /// Цикл выполняется следующим образом:
    /// 1. Агент вызывается с указанными входными данными.
    /// 2. Если есть конечный вывод (т.е. агент выдаёт что-то типа `agent.outputType`), цикл завершается.
    /// 3. Если происходит передача управления, мы снова запускаем цикл с новым агентом.
    /// 4. В противном случае мы выполняем вызовы инструментов (если таковые имеются) и перезапускаем цикл.
    ///
    /// В двух случаях агент может сгенерировать исключение:
    /// 1. При превышении максимального количества ходов (maxTurns) возникает исключение MaxTurnsExceeded.
    /// 2. При срабатывании ограничительного барьера (guardrail tripwire) возникает исключение GuardrailTripwireTriggered.
    /// Обратите внимание, что запускаются только входные ограничительные барьеры первого агента.
    ///
    /// @param startingAgent Начальный агент для запуска.
    /// @param input         Начальные входные данные для агента. Вы можете передать одну строку для пользовательского сообщения
    ///                                                                                                                                                                                                                                                                                                                                                                                                                                                      или список входных элементов.
    /// @param agentContext  Контекст для запуска агента.
    /// @param maxTurns      Максимальное количество ходов для запуска агента. Ход определяется как один
    ///                                                                                                                                                                                                                                                                                                                                                                                                                                                      вызов ИИ (включая любые возможные вызовы инструментов).
    /// @param agentHooks    объект, принимающий обратные вызовы при различных событиях жизненного цикла.
    /// @param runConfig     глобальные настройки для всего запуска агента.
    /// @return результат запуска, содержащий все входные данные, результаты проверки и выходные данные последнего
    ///         агента. Агенты могут выполнять передачи, поэтому мы не знаем конкретный тип выходных данных.
    public RunResult<T> run(Agent startingAgent,
                            List<ResponseInputItem> input,
                            AgentContext agentContext,
                            Integer maxTurns,
                            AgentHooks agentHooks,
                            RunConfig runConfig) {
        AgentToolUseTracker toolUseTracker = new AgentToolUseTracker();
        int currentTurn = 0;
        List<ResponseInputItem> originalInput = input;
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


            switch (turnResult.getNextStep()) {
                case NextStepFinalOutput(Object output) -> {
                    //Финальный статус
                    T finalOutput = (T) output;

                    return RunResult.<T>builder()
                            .input(originalInput)
                            .newItems(generatedItems)
                            .rawResponses(modelResponses)
                            .finalOutput(finalOutput)
                            .lastAgent(currentAgent)
                            .context(agentContext)
                            .build();
                }
                case NextStepHandoff(Agent newAgent) -> {
                    currentAgent = newAgent;
                    shouldRunAgentStartHooks = true;
                    log.debug("Передаем управление агенту \"{}\"", currentAgent.getName());
                }
                case NextStepRunAgain nextStepRunAgain -> log.debug("Следующая итерация");
                default ->
                        throw new AgentsException("Unknown next step type: %s"
                                .formatted(turnResult.getNextStep().getClass()));
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

        List<Handoff> handoffs = getHandoffs(agent, agentContext, runConfig);
        //Преобразуем входящие элементы в понятный формат
        List<ResponseInputItem> input = new ArrayList<>(originalInput);
        //Преобразуем входящие действия в формат который можно передать LLM
        generatedItems.forEach(g -> input.add(g.makeInputItem()));

        AgentClientSpring agentModel = new AgentClientSpring(runConfig);

        ModelResponse newResponse = agentModel.call(
                agent,
                systemPrompt,
                input,
                outputSchema,
                tools,
                handoffs,
                agentContext,
                agentHooks
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

    private List<Handoff> getHandoffs(Agent agent, AgentContext agentContext, RunConfig runConfig) {
        List<Handoff> handoffs = new ArrayList<>(agent.getHandoffs());
        for (Agent agentHandoff : agent.getHandoffAgents()) {
            handoffs.add(Handoff.builder().agent(agentHandoff)
                    .inputFilter(runConfig.getHandoffInputFilter())
                    .build());
        }

        return handoffs.stream()
                .filter(h -> h.getEnabled().test(agentContext, agent))
                .toList();
    }


}
