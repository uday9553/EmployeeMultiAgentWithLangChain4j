package com.uday.sample.chat;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface PlanningWorkflow {
    @Agent
    ChatTypes.Plan plan(@V("message") String message, @V("employee") String employee, @V("question") String question);
}
