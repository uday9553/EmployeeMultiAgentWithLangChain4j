package com.uday.sample;

import com.uday.sample.chat.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import org.mockito.ArgumentCaptor;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PlanningWorkflowTests {
    @Test
    void installedAgenticSequenceParsesStructuredPlan() {
        var model = mock(ChatModel.class, CALLS_REAL_METHODS);
        doReturn(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA)).when(model).supportedCapabilities();
        doReturn(ChatResponse.builder().aiMessage(AiMessage.from("""
                {"steps":[{"operation":"LIST","employee":null,"fields":{}}],"question":null}
                """)).build()).when(model).chat(any(ChatRequest.class));
        var agent = AgenticServices.agentBuilder(IntentAgent.class).chatModel(new PlanChatModel(model)).outputKey("operationPlan").build();
        var workflow = AgenticServices.sequenceBuilder(PlanningWorkflow.class).subAgents(agent).outputKey("operationPlan").build();
        assertEquals(ChatTypes.Operation.LIST, workflow.plan("Show all employees", "none", "").steps().getFirst().operation());
        var request = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model, times(1)).chat(request.capture());
        var format = request.getValue().parameters().responseFormat();
        assertNotNull(format);
        assertEquals(ResponseFormatType.JSON, format.type());
        assertNotNull(format.jsonSchema());
        var schema = (dev.langchain4j.model.chat.request.json.JsonRawSchema) format.jsonSchema().rootElement();
        var json = new tools.jackson.databind.ObjectMapper().readTree(schema.schema());
        assertEquals(6, json.at("/properties/steps/maxItems").asInt());
        assertEquals(1, json.at("/properties/steps/minItems").asInt());
        assertFalse(json.at("/properties/steps/items/properties/fields/additionalProperties").asBoolean());
    }

    private PlanningWorkflow liveWorkflow() {
        var model = dev.langchain4j.model.ollama.OllamaChatModel.builder()
                .baseUrl("http://localhost:11434").modelName(System.getProperty("ollama.test-model", "qwen2.5:3b"))
                .temperature(0.0).numPredict(768).maxRetries(0)
                .timeout(java.time.Duration.ofSeconds(180)).build();
        var agent = AgenticServices.agentBuilder(IntentAgent.class)
                .chatModel(new PlanChatModel(model)).outputKey("operationPlan").build();
        return AgenticServices.sequenceBuilder(PlanningWorkflow.class)
                .subAgents(agent).outputKey("operationPlan").build();
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "ollama.live", matches = "true")
    void liveOllamaListsEmployeesWithoutExtraOperations() {
        var plan = liveWorkflow().plan("show all employees", "none", "");
        assertEquals(1, plan.steps().size(), plan.toString());
        assertEquals(ChatTypes.Operation.LIST, plan.steps().getFirst().operation());
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "ollama.live", matches = "true")
    void liveOllamaExtractsCreateRequestWithoutLookup() {
        var plan = liveWorkflow().plan(
                "Add new employee with name Meghana department is IT and salary is 50000", "EMP-previous", "");
        assertEquals(1, plan.steps().size(), plan.toString());
        var step = plan.steps().getFirst();
        assertEquals(ChatTypes.Operation.CREATE, step.operation());
        assertEquals(java.util.Map.of("name", "Meghana", "department", "IT", "salary", "50000"), step.fields());
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "ollama.live", matches = "true")
    void liveOllamaClarifiesMissingCreateFields() {
        var plan = liveWorkflow().plan("Add a new employee named Meghana", "none", "");
        assertEquals(1, plan.steps().size(), plan.toString());
        var planner = mock(PlanningWorkflow.class);
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(plan);
        var employees = mock(com.uday.sample.service.EmployeeService.class);
        var promotions = mock(com.uday.sample.service.PromotionService.class);
        var service = new ConversationService(planner, employees, promotions, new UiPlanner(),
                new AnalyticsAgent(employees, promotions), new ManagementAgent(employees), mock(ConversationStore.class));
        var response = service.chat(new ChatTypes.Request("live-missing-fields", "1", "Add a new employee named Meghana"));
        assertEquals("FOLLOW_UP", response.status(), plan.toString());
        verifyNoInteractions(employees);
    }

    @Test
    void routesOnlyCompleteListingCommandsWithoutInvokingModel() {
        var delegate = mock(PlanningWorkflow.class);
        var workflow = new RoutingPlanningWorkflow(delegate);
        var plan = workflow.plan("Show all employees", "none", "");
        assertEquals(1, plan.steps().size());
        assertEquals(ChatTypes.Operation.LIST, plan.steps().getFirst().operation());
        verifyNoInteractions(delegate);

        workflow.plan("show all employees and create Rahul", "none", "previous question");
        verify(delegate).plan("show all employees and create Rahul", "none", "previous question");
    }
}
