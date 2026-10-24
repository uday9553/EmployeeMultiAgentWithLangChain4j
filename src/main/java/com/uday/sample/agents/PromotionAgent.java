package com.uday.sample.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface PromotionAgent {
    @UserMessage("""
            Employee request: {{request}}
            Employee details: {{employeeDetails}}
            Use the employee ID from the fetched details to call the promotion tool.
            Return only the tool's eligibility decision and policy explanation.
            Do not calculate or invent eligibility yourself.
            If employee details are unavailable, explain that eligibility cannot be checked.
            """)
    @Agent(outputKey = "promotionDecision", description = "Checks promotion eligibility")
    String checkPromotion(@V("request") String request,
                          @V("employeeDetails") String employeeDetails);
}
