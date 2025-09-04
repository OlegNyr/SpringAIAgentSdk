package ru.nyrk.agents;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import ru.nyrk.agents.runner.DefaultAgentRunner;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static ru.nyrk.client.ClientFactoryFactory.STUB;

@Slf4j
@WireMockTest(httpPort = 3000)
public class HelloFunctionTest extends Config {

    @Test
    void test1() {
        String systemPrompt = "You only respond in haikus.";

        mockSystem(containing(systemPrompt), "hellofunction.json");
        mockSystemAndAssistant(containing(systemPrompt), "hellofunction2.json");

        Agent agent = Agent.builder()
                .name("Assistant")
                .instructions(systemPrompt)
                .tools(List.of(ToolCallbacks.from(new CustomTools())))
                .build();

        AgentRunner agentRunner = new DefaultAgentRunner(makeModel(STUB));

        RunResult runResult = agentRunner.run(agent, "Какая погода в Токио?");

        Assertions.assertEquals("Токио сегодня сияет\nСолнце светит ярко в небе\nХороший день для прогулок", runResult.getFinalOutput());
    }


}
