package com.uday.sample.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface EmployeeWorkflowAgent {
    @Agent
    String handle(@V("request") String request);
}
