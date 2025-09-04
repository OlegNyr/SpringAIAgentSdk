package ru.nyrk.client.deepseek;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.web.client.RestClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;
import ru.nyrk.client.ChatApi;
import ru.nyrk.client.ChatClientFactory;

public class DeepseekClientFactory implements ChatClientFactory<DeepSeekApi> {
    @Override
    public ChatApi<DeepSeekApi> makeChatApi() {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(System.getenv("DEEPSEEK_API"))
                .restClientBuilder(RestClient.builder()
                        .requestInterceptor(new LogbookClientHttpRequestInterceptor(Logbook.builder().build())))
                .build();
        return new DeepSeekApiMore(deepSeekApi);
    }

    @Override
    public ChatClient makeClient(ChatApi<?> chatApi) {
        ChatModel deepSeekChatModel = makeModel(chatApi);

        return ChatClient.create(deepSeekChatModel);
    }

    public ChatModel makeModel(ChatApi<?> chatApi) {
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(chatApi == null ? makeChatApi().origin() : (DeepSeekApi) chatApi.origin())
                .build();
        return deepSeekChatModel;
    }
}
