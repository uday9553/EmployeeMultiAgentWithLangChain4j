package com.uday.sample.agentsConfig;

import com.uday.sample.agents.*;
import com.uday.sample.tool.EmployeeTool;
import com.uday.sample.tool.PromotionTool;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeAgentConfig {
    @Bean
    public EmployeeAgent employeeAgent(ChatModel chatModel, EmployeeTool employeeTool) {
        return AgenticServices.agentBuilder(EmployeeAgent.class)
                .chatModel(chatModel).tools(employeeTool).outputKey("employeeDetails").build();
    }

    @Bean
    public PromotionAgent promotionAgent(ChatModel chatModel, PromotionTool promotionTool) {
        return AgenticServices.agentBuilder(PromotionAgent.class)
                .chatModel(chatModel).tools(promotionTool).outputKey("promotionDecision").build();
    }

    @Bean
    public EmployeeResponseAgent employeeResponseAgent(ChatModel chatModel) {
        return AgenticServices.agentBuilder(EmployeeResponseAgent.class)
                .chatModel(chatModel).outputKey("employeeFinalResponse").build();
    }

    @Bean
    public EmployeeWorkflowAgent employeeWorkflowAgent(EmployeeAgent employeeAgent,
                                                       PromotionAgent promotionAgent,
                                                       EmployeeResponseAgent employeeResponseAgent) {
        return AgenticServices.sequenceBuilder(EmployeeWorkflowAgent.class)
                .subAgents(employeeAgent, promotionAgent, employeeResponseAgent)
                .outputKey("employeeFinalResponse").build();
    }
}
