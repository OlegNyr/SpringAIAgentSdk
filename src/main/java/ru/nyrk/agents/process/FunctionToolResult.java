package ru.nyrk.agents.process;

import lombok.Builder;
import lombok.Value;
import org.springframework.ai.tool.ToolCallback;
import ru.nyrk.agents.RunItem;


@Value
@Builder
public class FunctionToolResult {

    /**
     * The tool that was run.
     */
    ToolCallback tool;

    /**
     * The output of the tool.
     */
    String result;

    /**
     * The run item that was produced as a result of the tool call.
     */
    RunItem<?> runItem;
}
