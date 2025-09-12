package ru.nyrk.agents;

import chat.giga.springai.GigaChatModel;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static ru.nyrk.client.ClientFactoryFactory.GIGACHAT;

public class TemplatesTest extends Config {
    @Test
    void name() {
        ChatModel chatModel = makeModel(GIGACHAT);
        ObservationRegistry observationRegistry = ObservationRegistry.create();

        String content = ChatClient.create(chatModel, observationRegistry)
                .prompt("hello")
                .advisors()
                .user(p -> p.text("Welcome to {name}!").param("name", "GigaChat"))
                .call()
                .content();
        System.out.println(content);
    }

    @Test
    void name2() {
        ObservationRegistry observationRegistry = ObservationRegistry.create();

        Observation observation = Observation.createNotStarted("foo", observationRegistry)
                .lowCardinalityKeyValue("lowTag", "lowTagValue")
                .highCardinalityKeyValue("highTag", "highTagValue");
        observation.observe(() -> {
            observation.event(Observation.Event.of("event1"));
            System.out.println("Hello");
        });

        System.out.println(observationRegistry);

    }
}
