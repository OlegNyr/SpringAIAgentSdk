package ru.nyrk.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public interface ChatClientFactory<T> {
    ChatApi<T> makeChatApi();

    ChatClient makeClient(ChatApi<?> chatApi);

    ChatModel makeModel(ChatApi<?> chatApi);
}
