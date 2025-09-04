package ru.nyrk.agents;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.springframework.ai.chat.model.ChatModel;
import ru.nyrk.client.ChatClientFactory;
import ru.nyrk.client.ClientFactoryFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class Config {

    public static ChatModel makeModel(String llm) {
        ChatClientFactory<?> chatClientFactory = ClientFactoryFactory.chatClientFactory(llm);
        return chatClientFactory.makeModel(null);
    }

    public static void mockSystem(StringValuePattern containing, String fileName) {
        stubFor(post("/chat/completions")
                .withRequestBody(
                        and(
                                matchRoleSystemContent(containing),
                                not(mathRoleAsistant())
                        )
                )
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(fileName)
                )
        );
    }

    public static void mockSystemAndAssistant(StringValuePattern containing, String fileName) {
        stubFor(post("/chat/completions")
                .withRequestBody(
                        and(
                                matchRoleSystemContent(containing),
                                mathRoleAsistant()
                        ))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(fileName)
                )
        );
    }

    private static StringValuePattern matchRoleSystemContent(StringValuePattern containing) {
        return matchingJsonPath("$.messages[?(@.role == 'system')].content", containing);
    }

    private static StringValuePattern mathRoleAsistant() {
        return matchingJsonPath("$.messages[?(@.role == 'assistant')].content");
    }
}
