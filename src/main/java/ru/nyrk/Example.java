package ru.nyrk;

import org.springframework.ai.chat.model.ChatModel;
import ru.nyrk.agents.Agent;
import ru.nyrk.agents.AgentRunner;
import ru.nyrk.agents.RunResult;
import ru.nyrk.agents.item.input.EasyInputMessageParam;
import ru.nyrk.agents.item.Role;
import ru.nyrk.agents.runner.DefaultAgentRunner;
import ru.nyrk.client.ChatClientFactory;
import ru.nyrk.client.ClientFactoryFactory;

public class Example {
    public static void main(String[] args) {
        ChatClientFactory<?> chatClientFactory = ClientFactoryFactory.chatClientFactory(ClientFactoryFactory.DEEPSEEK);
        ChatModel chatModel = chatClientFactory.makeModel(null);

        Agent historyTutorAgent = Agent.builder()
                .name("History Tutor")
                .handoffDescription("Specialist agent for historical questions")
                .instructions("You provide assistance with historical queries. Explain important events and context clearly.")
                .build();

        Agent mathTutorAgent = Agent.builder()
                .name("Math Tutor")
                .handoffDescription("Specialist agent for math questions")
                .instructions("You provide help with math problems. Explain your reasoning at each step and include examples")
                .build();

        Agent triageAgent = Agent.builder()
                .name("Triage Agent")
                .instructions("You determine which agent to use based on the user's homework question")
                .agent(historyTutorAgent)
                .agent(mathTutorAgent)
                .build();


        EasyInputMessageParam input = EasyInputMessageParam.builder()
                .content("What is the capital of France?")
                .role(Role.USER)
                .build();

        AgentRunner agentRunner = new DefaultAgentRunner(chatModel);

        RunResult<?> runResult = agentRunner.run(triageAgent, "What is the capital of France?");

        System.out.println(runResult.getFinalOutput());
    }
}
