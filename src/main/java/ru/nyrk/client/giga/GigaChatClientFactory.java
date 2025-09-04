package ru.nyrk.client.giga;

import chat.giga.springai.GigaChatModel;
import chat.giga.springai.GigaChatOptions;
import chat.giga.springai.api.auth.GigaChatApiProperties;
import chat.giga.springai.api.auth.GigaChatInternalProperties;
import chat.giga.springai.api.chat.GigaChatApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;
import ru.nyrk.client.ChatApi;
import ru.nyrk.client.ChatClientFactory;

import java.util.Base64;

import static chat.giga.springai.api.auth.GigaChatApiProperties.GigaChatApiScope.GIGACHAT_API_PERS;

public class GigaChatClientFactory implements ChatClientFactory<GigaChatMoreApi> {

    public ChatClient makeClient(ChatApi<?> chatApi) {
        return ChatClient.create(makeModel(chatApi));
    }

    public ChatApi<GigaChatMoreApi> makeChatApi() {

        String gigaApi = new String(Base64.getDecoder().decode(System.getenv("GIGA_API")));
        String[] split = gigaApi.split(":");

        GigaChatApiProperties gigaChatApiProperties = GigaChatApiProperties.builder()
                .clientId(split[0])
                .clientSecret(split[1])
                .scope(GIGACHAT_API_PERS)
                .unsafeSsl(true)
                .authUrl("https://developers.sber.ru/docs/api/gigachat/auth/v2/oauth")
                .build();

        return new GigaChatMoreApi(gigaChatApiProperties,
                RestClient.builder()
                        .requestInterceptor(new LogbookClientHttpRequestInterceptor(Logbook.builder().build())),
                WebClient.builder(),
                RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER
        );
    }

    @Override
    public ChatModel makeModel(ChatApi<?> chatApi) {
        GigaChatOptions chatOptions = GigaChatOptions.builder()
                .model(GigaChatApi.ChatModel.GIGA_CHAT_2_MAX)
                .build();
        return GigaChatModel.builder()
                .gigaChatApi(chatApi == null ? makeChatApi().origin() : (GigaChatApi) chatApi.origin())
                .defaultOptions(chatOptions)
                .internalProperties(new GigaChatInternalProperties())
                .build();
    }
}
