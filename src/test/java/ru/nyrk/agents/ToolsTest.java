package ru.nyrk.agents;

import io.micrometer.observation.Observation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.DefaultToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.ai.tool.resolution.TypeResolverHelper;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import ru.nyrk.client.ClientFactoryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class ToolsTest {
    public static Function<ParamsFunction, String> function = (ParamsFunction city) -> "The weather in %s is sunny.".formatted(city);

    @Test
    void test1() {
        ChatModel chatModel = Config.makeModel(ClientFactoryFactory.DEEPSEEK);

        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        FunctionToolCallback<ParamsFunction, String> getWeather = FunctionToolCallback
                .builder("getWeather", function)
                .inputSchema(JsonSchemaGenerator.generateForType(ParamsFunction.class))
                .inputType(ParameterizedTypeReference.forType(ParamsFunction.class))
                .build();

        List<ToolCallback> toolCallbacks = new ArrayList<>();
        toolCallbacks.addAll(Arrays.asList(ToolCallbacks.from(new CustomTools())));


        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(List.of(getWeather))
//                .toolCallbacks(List.copyOf(toolCallbacks))
                .internalToolExecutionEnabled(false)
                .build();

        Prompt prompt = Prompt.builder()
                .chatOptions(chatOptions)
                .messages(new SystemMessage("You only respond in haikus."),
                        new UserMessage("Какая погода в Токио?"))
                .build();

        ChatResponse chatResponse = chatModel.call(prompt);

        while (chatResponse.hasToolCalls()) {
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);

            prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);

            chatResponse = chatModel.call(prompt);
        }

        System.out.println(chatResponse.getResult().getOutput().getText());
    }


    @Data
    public static class ParamsFunction {
        String city;
    }

    @Test
    void name() {
        ParamsFunction s = JsonParser.fromJson("{\"city\": \"Tokyo\"}", ParamsFunction.class);
        System.out.println(s);
    }
}
