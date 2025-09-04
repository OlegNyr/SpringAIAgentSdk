package ru.nyrk.agents;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static ru.nyrk.client.ClientFactoryFactory.GIGACHAT;

@Slf4j
@WireMockTest(httpPort = 3000)
public class HellWorldTest extends Config {

    @Test
    void test1() throws InterruptedException {
        mockSystem(containing("You are a helpful assistant"), "helloworld.json");

        Agent agent = Agent.builder()
                .name("Assistant")
                .instructions("You are a helpful assistant")
                .build();

        var agentRunner = AgentRunners.runner().model(makeModel(GIGACHAT));

        RunResult<String> runResult = agentRunner.run(agent, "Write a haiku about recursion in programming.");

        Assertions.assertEquals("A function calls itself,\nEndless loop without an exit,\nBase case brings it home.", runResult.getFinalOutput());
    }

}
