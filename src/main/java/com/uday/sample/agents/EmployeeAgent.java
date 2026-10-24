package com.uday.sample.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface EmployeeAgent {
    @UserMessage("""
            Employee request: {{request}}
            Extract the full employee ID and use the employee tool to fetch real details.
            Return the complete employee details, including employee ID, designation,
            months in role and performance rating. Do not invent missing information.
            If no employee ID is provided, state that it is required.
            """)
    @Agent(outputKey = "employeeDetails", description = "Fetches employee details")
    String checkEmployee(@V("request") String request);
}
