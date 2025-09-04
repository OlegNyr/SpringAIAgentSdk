package ru.nyrk.agents;

import ru.nyrk.agents.item.input.FunctionCallOutput;
import ru.nyrk.agents.models.ResponseFunctionToolCall;

public class ItemHelpers {


    public static FunctionCallOutput toolCallOutputItem(ResponseFunctionToolCall responseFunctionToolCall,
                                                        String result) {
        return FunctionCallOutput.builder()
                .callId(responseFunctionToolCall.getCallId())
                .name(responseFunctionToolCall.getName())
                .result(result)
                .build();
    }

}
