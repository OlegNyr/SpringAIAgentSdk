package ru.nyrk.agents.process;

import ru.nyrk.agents.Handoff;
import ru.nyrk.agents.models.ResponseFunctionToolCall;

record ToolRunHandoff(ResponseFunctionToolCall toolCall, Handoff handoff) {
}
