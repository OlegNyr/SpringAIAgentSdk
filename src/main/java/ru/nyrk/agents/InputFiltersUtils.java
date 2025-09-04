package ru.nyrk.agents;

import ru.nyrk.agents.item.HandoffCallItem;
import ru.nyrk.agents.item.HandoffOutputItem;
import ru.nyrk.agents.item.ToolCallItem;
import ru.nyrk.agents.item.ToolCallResultItem;
import ru.nyrk.agents.item.input.FunctionCallOutput;
import ru.nyrk.agents.item.input.ResponseOutputMessageParam;
import ru.nyrk.agents.process.HandoffInputData;

import java.util.function.Predicate;

public class InputFiltersUtils {

    public static HandoffInputData removeAllTools(HandoffInputData inputData) {

        var filteredHistory = inputData.getInputHistory()
                .stream()
                .filter(Predicate.not(InputFiltersUtils::isInputTooling))
                .toList();

        var filteredPreHandoffItems = inputData.getPreHandoffItems()
                .stream()
                .filter(Predicate.not(InputFiltersUtils::isRunningToolAndHandoff))
                .toList();

        var filteredNewItems = inputData.getNewItems()
                .stream()
                .filter(Predicate.not(InputFiltersUtils::isRunningToolAndHandoff))
                .toList();

        return HandoffInputData.builder()
                .inputHistory(filteredHistory)
                .preHandoffItems(filteredPreHandoffItems)
                .newItems(filteredNewItems)
                .runContext(inputData.getRunContext())
                .build();
    }

    private static boolean isRunningToolAndHandoff(RunItem<?> runItem) {
        return runItem instanceof ToolCallItem
                || runItem instanceof ToolCallResultItem
                || runItem instanceof HandoffOutputItem
                || runItem instanceof HandoffCallItem;
    }

    private static boolean isInputTooling(ResponseInputItem responseInputItem) {
        return responseInputItem instanceof FunctionCallOutput
                || responseInputItem instanceof ResponseOutputMessageParam;
    }
}
