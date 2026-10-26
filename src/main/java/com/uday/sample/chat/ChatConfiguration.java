package com.uday.sample.chat;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.context.annotation.*;

@Configuration
public class ChatConfiguration {
    @Bean
    IntentAgent intentAgent(ChatModel model) {
        return AgenticServices.agentBuilder(IntentAgent.class).chatModel(new PlanChatModel(model)).outputKey("operationPlan").build();
    }

    @Bean
    PlanningWorkflow planningWorkflow(IntentAgent agent) {
        var workflow = AgenticServices.sequenceBuilder(PlanningWorkflow.class).subAgents(agent).outputKey("operationPlan").build();
        return new RoutingPlanningWorkflow(workflow);
    }
}
