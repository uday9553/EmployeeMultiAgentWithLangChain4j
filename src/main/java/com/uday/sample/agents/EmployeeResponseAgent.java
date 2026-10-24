package com.uday.sample.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface EmployeeResponseAgent {
    @UserMessage("""
            Original request: {{request}}
            Employee details: {{employeeDetails}}
            Promotion decision: {{promotionDecision}}
            Create a short, clear response explaining promotion eligibility and the policy.
            Do not invent information or claim that a promotion has been applied.
            """)
    @Agent(outputKey = "employeeFinalResponse", description = "Creates the employee promotion response")
    String createResponse(@V("request") String request,
                          @V("employeeDetails") String employeeDetails,
                          @V("promotionDecision") String promotionDecision);
}
