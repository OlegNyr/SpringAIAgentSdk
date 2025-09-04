package ru.nyrk.agents.process;

import org.springframework.ai.tool.ToolCallback;
import ru.nyrk.agents.models.ResponseFunctionToolCall;

record ToolRunFunction(ResponseFunctionToolCall toolCall,
                              ToolCallback toolCallback) {
}
