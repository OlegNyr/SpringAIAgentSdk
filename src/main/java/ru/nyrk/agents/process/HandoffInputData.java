package ru.nyrk.agents.process;

import lombok.Builder;
import lombok.Value;
import ru.nyrk.agents.AgentContext;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.RunItem;

import java.util.List;

@Value
@Builder
public class HandoffInputData {
    List<ResponseInputItem> inputHistory;
    List<RunItem<?>> preHandoffItems;
    List<RunItem<?>> newItems;
    AgentContext runContext;
}
