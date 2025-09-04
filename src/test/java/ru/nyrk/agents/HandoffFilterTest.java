package ru.nyrk.agents;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import ru.nyrk.agents.item.Role;
import ru.nyrk.agents.item.input.EasyInputMessageParam;
import ru.nyrk.agents.process.HandoffInputData;
import ru.nyrk.agents.runner.DefaultAgentRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static ru.nyrk.client.ClientFactoryFactory.DEEPSEEK;
import static ru.nyrk.client.ClientFactoryFactory.STUB;

@WireMockTest(httpPort = 3000)
public class HandoffFilterTest extends Config {


    public record ParamFunction(Integer max) {

    }

    @Test
    void test1() {
        Agent firstAgent = Agent.builder()
                .name("Assistant")
                .instructions("Be extremely concise.")
                .toolFunction(a -> new Random().nextInt(a.max()), ParamFunction.class)
                .name("randomNumberTool")
                .description("Return a random integer between 0 and the given maximum.")
                .and()
                .build();

        // Spanish agent
        Agent spanishAgent = Agent.builder()
                .name("Spanish Assistant")
                .instructions("You only speak Spanish and are extremely concise.")
                .handoffDescription("A Spanish-speaking assistant.")
                .build();

        Function<HandoffInputData, HandoffInputData> inputFilter = (HandoffInputData inputData) -> {
            HandoffInputData handoffMessageData = InputFiltersUtils.removeAllTools(inputData);

            return HandoffInputData.builder()
                    .inputHistory(sublist(handoffMessageData.getInputHistory(), 2))
                    .preHandoffItems(handoffMessageData.getPreHandoffItems())
                    .newItems(handoffMessageData.getNewItems())
                    .build();
        };

        Agent secondAgent = Agent.builder()
                .name("Assistant")
                .instructions("Be a helpful assistant. If the user speaks Spanish, handoff to the Spanish assistant.")
                .handoff(Handoff.builder().agent(spanishAgent).inputFilter(inputFilter).build())
                .build();

        AgentRunner agentRunner = new DefaultAgentRunner(makeModel(STUB));

        mockSystem(containing("Be extremely concise."), "handoffFilterTest1.json");

        var result = agentRunner.run(firstAgent, "Привет, меня зовут Сара");


        System.out.println("Step 1 done");

        AgentRunner agentRunnerDepseek = new DefaultAgentRunner(makeModel(DEEPSEEK));
        result = agentRunnerDepseek.run(
                firstAgent,
                join(result.makeInputList(),
                        new EasyInputMessageParam("Can you generate a random number between 0 and 100?", Role.USER))
        );

        System.out.println("Step 2 done");
        result = agentRunnerDepseek.run(
                secondAgent,
                join(result.makeInputList(),
                        new EasyInputMessageParam("I live in New York City. Whats the population of the city?", Role.USER))
        );
        System.out.println("Step 3 done");
        result = agentRunnerDepseek.run(
                secondAgent,
                join(result.makeInputList(),
                        new EasyInputMessageParam("Por favor habla en español. ¿Cuál es mi nombre y dónde vivo?", Role.USER))
        );
        System.out.println("Step 4 done");

        for (ResponseInputItem item : result.getInput()) {
            System.out.println(item);
        }

    }

    private List<ResponseInputItem> sublist(List<ResponseInputItem> inputHistory, int start) {
        if (start >= inputHistory.size()) {
            return List.of();
        }
        return List.copyOf(inputHistory.subList(start, inputHistory.size()));

    }

    private static <T> List<T> join(List<T> input, T item) {
        List<T> result = new ArrayList<>(input);
        result.add(item);
        return Collections.unmodifiableList(result);
    }

}
