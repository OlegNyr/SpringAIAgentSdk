package ru.nyrk.agents.runner;

import ru.nyrk.agents.Agent;

public record NextStepHandoff(Agent newAgent) implements NextStepResult {
}
