package ru.nyrk.agents;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static ru.nyrk.client.ClientFactoryFactory.*;

@Slf4j
@WireMockTest(httpPort = 3000)
public class HelloJsonTest extends Config {


    @Test
    void test1() {

        Agent storyOutlineAgent = Agent.builder()
                .name("story_outline_agent")
                .instructions("Generate a very short story outline based on the user's input.")
                .build();
        mockSystem(containing("Generate a very short story outline based on the user's input."), "hellojson1.json");

        Agent outlineCheckerAgent = Agent.builder()
                .name("outline_checker_agent")
                .instructions("Read the given story outline, and judge the quality. Also, determine if it is a scifi story.")
                .outputType(OutlineCheckerOutput.class)
                .build();
        mockSystem(containing("Read the given story outline, and judge the quality. Also, determine if it is a scifi story."), "hellojson2.json");

        Agent storyAgent = Agent.builder()
                .name("story_agent")
                .instructions("Write a short story based on the given outline.")
                .build();
        mockSystem(containing("Write a short story based on the given outline."), "hellojson3.json");

        var agentRunner = AgentRunners.runner().model(makeModel(DEEPSEEK));



        RunResult<String> outlineResult = agentRunner
                .copy()
                .temperature(1.0D)
                .modelName("deepseek-reasoner")
                .run(storyOutlineAgent, "История о жизни мальчика, в большой ИТ компании будущего с ИИ");

        System.out.println("Outline generated");
        RunResult<OutlineCheckerOutput> outlineCheckerResult = agentRunner.copy()
                .model(makeModel(GIGACHAT))
                .run(outlineCheckerAgent, outlineResult.getFinalOutput());

        if (!outlineCheckerResult.getFinalOutput().goodQuality()) {
            Assertions.fail("Качество плана не очень хорошее, поэтому на этом остановимся.");
        }

        if (!outlineCheckerResult.getFinalOutput().scifi()) {
            Assertions.fail("План — это не научно-фантастический рассказ, поэтому мы на этом остановимся.");
        }

        System.out.println("План качественный и представляет собой научно-фантастическую историю, поэтому мы продолжаем писать ее.");
        var storyResult = agentRunner
                .copy()
                .maxTokens(3000)
                .run(storyAgent, outlineResult.getFinalOutput());
        Assertions.assertEquals("### Код счастья\n\nАртём щёлкнул Enter", storyResult.getFinalOutput());
    }

    record OutlineCheckerOutput(boolean goodQuality, boolean scifi) {
    }
}
