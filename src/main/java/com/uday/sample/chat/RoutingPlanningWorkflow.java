package com.uday.sample.chat;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Recognizes complete, unambiguous listing commands; delegates all other requests. */
public final class RoutingPlanningWorkflow implements PlanningWorkflow {
    private final PlanningWorkflow delegate;

    public RoutingPlanningWorkflow(PlanningWorkflow delegate) {
        this.delegate = delegate;
    }

    @Override
    public ChatTypes.Plan plan(String message, String employee, String question) {
        String command = message == null ? "" : message.strip().toLowerCase(Locale.ROOT);
        if (command.matches("(?:show|list|get) all employees[.!?]?")) {
            return new ChatTypes.Plan(List.of(new ChatTypes.Step(ChatTypes.Operation.LIST, null, Map.of())), null);
        }
        return delegate.plan(message, employee, question);
    }
}
