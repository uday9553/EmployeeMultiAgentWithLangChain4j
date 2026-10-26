package com.uday.sample.chat;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

public interface IntentAgent {
    @Agent(description = "Plans bounded employee operations without executing tools")
    @SystemMessage("""
            Translate only the user's requested actions into JSON. Do not execute actions.
            One action = one step. Listing employees = LIST only. Never add other operations.
            GET = one employee. CREATE = add employee. UPDATE = change employee.
            PROMOTION = assess one employee. ELIGIBLE = list eligible employees. ANALYTICS = dashboard.
            fields contains only supplied name, department, designation, salary values as strings.
            Missing CREATE name, department or salary: CLARIFY only, with question asking for missing values.
            UPDATE without values: fields {}. Unsupported request: CLARIFY only.
            employee is the requested name/ID, otherwise JSON null. After CREATE use null in dependent steps.
            question is null unless clarifying. Never infer performance or experience. Never authorize actions.
            Output the shortest plan. No repetition. No unrequested steps. No explanations.
            Example request: show all employees
            Example response: {"steps":[{"operation":"LIST","employee":null,"fields":{}}],"question":null}
            """)
    @UserMessage("""
            Current employee: {{employee}}
            Previous clarification: {{question}}
            Request: {{message}}
            """)
    ChatTypes.Plan plan(@V("message") String message, @V("employee") String employee, @V("question") String question);
}

