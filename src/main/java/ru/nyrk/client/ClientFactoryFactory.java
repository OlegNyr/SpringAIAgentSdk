package ru.nyrk.client;

import ru.nyrk.client.deepseek.DeepseekClientFactory;
import ru.nyrk.client.stub.StubClientFactory;

public class ClientFactoryFactory {

    public static final String DEEPSEEK = "deepseek";
    public static final String STUB = "stub";

    public static ChatClientFactory<?> chatClientFactory(String llm) {

        if (llm.equalsIgnoreCase(DEEPSEEK)) {
            return new DeepseekClientFactory();
        } else if (llm.equalsIgnoreCase(STUB)) {
            return new StubClientFactory();
        } else {
            throw new IllegalArgumentException("Unknown chat client factory: " + llm);
        }
    }
}
