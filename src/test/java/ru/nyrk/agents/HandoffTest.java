package ru.nyrk.agents;

import org.junit.jupiter.api.Test;
import ru.nyrk.agents.runner.DefaultAgentRunner;

import static ru.nyrk.client.ClientFactoryFactory.DEEPSEEK;

public class HandoffTest extends Config {
    @Test
    void name() {

        Agent historyTutorAgent = Agent.builder()
                .name("History Tutor")
                .handoffDescription("Specialist agent for historical questions")
                .instructions("You provide assistance with historical queries. Explain important events and context clearly.")
                .build();

        Agent mathTutorAgent = Agent.builder()
                .name("Math Tutor")
                .instructions("You provide help with math problems. Explain your reasoning at each step and include examples")
                .handoffDescription("Specialist agent for math questions")
                .build();

        Agent triageAgent = Agent.builder()
                .name("Triage Agent")
                .instructions("You determine which agent to use based on the user's homework question")
                .agent(historyTutorAgent)
                .agent(mathTutorAgent)
                .build();

        AgentRunner agentRunner = new DefaultAgentRunner(makeModel(DEEPSEEK));

        RunResult runResult = agentRunner.run(triageAgent, "кто был первым президентом Соединенных Штатов?");

        System.out.println(runResult.getFinalOutput());
    }
}
