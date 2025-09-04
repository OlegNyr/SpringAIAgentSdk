package ru.nyrk.agents.runner;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import ru.nyrk.agents.ModelResponse;
import ru.nyrk.agents.ResponseInputItem;
import ru.nyrk.agents.RunItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
@Builder
public class SingleStepResult {
    /**
     * The input items i.e. the items before run() was called. May be mutated by handoff input
     * filters.
     */
    @Singular("input")
    List<ResponseInputItem> originalInput;

    /**
     * The model response for the current step.
     */
    ModelResponse modelResponse;

    /**
     * Items generated before the current step.
     */
    @Singular("preStep")
    List<RunItem<?>> preStepItems;

    /**
     * Items generated during this current step.
     */
    @Singular("newStep")
    List<RunItem<?>> newStepItems;

    /**
     * следующий шаг, который нужно сделать.
     */
    NextStepResult nextStep;

    public List<RunItem<?>> unionGeneratedItems() {
        ArrayList<RunItem<?>> runItems = new ArrayList<>(preStepItems);
        runItems.addAll(newStepItems);
        return Collections.unmodifiableList(runItems);
    }
}
