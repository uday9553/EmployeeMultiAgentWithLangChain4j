package com.uday.sample;

import com.uday.sample.chat.*;
import com.uday.sample.service.*;
import com.uday.sample.model.Employee;
import org.junit.jupiter.api.*;

import java.util.*;

import static com.uday.sample.chat.ChatTypes.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ConversationServiceTests {
    PlanningWorkflow planner;
    EmployeeService employees;
    PromotionService promotions;
    ConversationService service;
    ManagementAgent management;

    @BeforeEach
    void setup() {
        planner = mock(PlanningWorkflow.class);
        employees = mock(EmployeeService.class);
        promotions = mock(PromotionService.class);
        management = new ManagementAgent(employees);
        service = new ConversationService(planner, employees, promotions, new UiPlanner(), new AnalyticsAgent(employees, promotions), management, mock(ConversationStore.class));
    }

    Employee employee() {
        var e = new Employee();
        e.setEmployeeId("EMP-real");
        e.setName("Rahul");
        return e;
    }

    @Test
    void createsThenUsesActualIdForPromotionAndDeduplicates() {
        var fields = Map.of("name", "Rahul", "department", "Engineering", "salary", "60000");
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(new Plan(List.of(new Step(Operation.CREATE, null, fields), new Step(Operation.PROMOTION, null, Map.of())), null));
        var employee = employee();
        when(employees.create(fields)).thenReturn(employee);
        when(employees.resolve("EMP-real")).thenReturn(employee);
        when(promotions.assess("EMP-real")).thenReturn(new PromotionService.Assessment("EMP-real", "Rahul", null, null, "Policy unavailable"));
        var request = new Request("conv", "one", "Create Rahul in Engineering with salary 60000 and check promotion");
        var result = service.chat(request);
        assertEquals(List.of("Employee Management", "Promotion"), result.agents());
        assertSame(result, service.chat(request));
        verify(employees, times(1)).create(fields);
        verify(promotions).assess("EMP-real");
    }

    @Test
    void isolatesConversationsAndRemembersEmployee() {
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(new Plan(List.of(new Step(Operation.GET, "Rahul", Map.of())), null));
        when(employees.resolve("Rahul")).thenReturn(employee());
        service.chat(new Request("conv", "1", "Show Rahul"));
        service.chat(new Request("conv", "2", "Show him"));
        verify(planner).plan("Show him", "EMP-real", "");
        service.chat(new Request("other-conv", "2", "Show him"));
        verify(planner).plan("Show him", "none", "");
    }

    @Test
    void sensitiveActionRequiresConfirmationAndIsBoundToConversation() {
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(new Plan(List.of(new Step(Operation.UPDATE, "Rahul", Map.of("salary", "75000"))), null));
        when(employees.resolve("Rahul")).thenReturn(employee());
        when(employees.update(anyString(), anyMap())).thenReturn(employee());
        var reply = service.chat(new Request("conv", "1", "Update salary to 75000"));
        var data = (Map<?, ?>) reply.ui().children().getFirst().props().get("data");
        String id = (String) data.get("actionId");
        assertThrows(IllegalArgumentException.class, () -> service.action(new ActionRequest("conv", id, Map.of("salary", "75000"), false)));
        assertThrows(IllegalArgumentException.class, () -> service.action(new ActionRequest("other-conv", id, Map.of("salary", "75000"), true)));
        var action = new ActionRequest("conv", id, Map.of("salary", "75000"), true);
        assertSame(service.action(action), service.action(action));
        verify(employees, times(1)).update("EMP-real", Map.of("salary", "75000"));
    }

    @Test
    void rejectsUnknownAndDeepUi() {
        var ui = new UiPlanner();
        assertThrows(IllegalArgumentException.class, () -> ui.layout(List.of(new Node("script", Map.of(), List.of()))));
        Node tree = new Node("text", Map.of(), List.of());
        for (int i = 0; i < 10; i++) tree = new Node("column", Map.of(), List.of(tree));
        final Node deep = tree;
        assertThrows(IllegalArgumentException.class, () -> ui.layout(List.of(deep)));
    }

    @Test
    void missingFieldsProduceFollowupWithoutWrites() {
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(new Plan(List.of(new Step(Operation.CREATE, null, Map.of("name", "Rahul"))), null));
        assertEquals("FOLLOW_UP", service.chat(new Request("conv", "1", "Create Rahul")).status());
        verifyNoInteractions(employees);
    }

    @Test
    void inventedCreateValuesProduceFollowupWithoutWrites() {
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(new Plan(List.of(
                new Step(Operation.CREATE, null, Map.of("name", "Meghana", "department", "IT", "salary", "50000"))), null));
        assertEquals("FOLLOW_UP", service.chat(new Request("conv", "invented", "Add an employee named Meghana")).status());
        verifyNoInteractions(employees);
    }
}
