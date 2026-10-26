package com.uday.sample;

import com.uday.sample.chat.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import java.net.*;
import java.net.http.*;
import java.util.*;

import static com.uday.sample.chat.ChatTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatIntegrationTests {
    @LocalServerPort
    int port;
    @MockitoBean
    PlanningWorkflow planner;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    ConversationStore store;
    final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();

    HttpResponse<String> post(String path, Object body, String accept) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/ai/" + path))
                .header("Accept", accept).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void completeChatActionsAnalyticsStreamingAndPersistence() throws Exception {
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(new Plan(List.of(new Step(Operation.CREATE, null, Map.of("name", "Integration Employee", "department", "Engineering", "salary", "60000")), new Step(Operation.PROMOTION, null, Map.of())), null));
        var request = new Request("integration", "create-1", "Create Integration Employee in Engineering with salary 60000 then check promotion");
        var created = post("chat", request, "application/json");
        assertEquals(200, created.statusCode(), created.body());
        var json = mapper.readTree(created.body());
        String id = json.get("results").get(0).get("employeeId").asText();
        assertEquals("employee-card", json.get("ui").get("children").get(0).get("type").asText());
        assertTrue(json.get("results").get(1).get("eligible").isNull());
        assertEquals(created.body(), post("chat", request, "application/json").body());
        assertEquals(id, store.load("integration").employee());
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(new Plan(List.of(new Step(Operation.UPDATE, null, Map.of("salary", "75000"))), null));
        var form = post("chat", new Request("integration", "update-1", "Update his salary to 75000"), "application/json");
        String action = mapper.readTree(form.body()).get("ui").get("children").get(0).get("props").get("data").get("actionId").asText();
        assertEquals(400, post("actions", new ActionRequest("integration", action, Map.of("salary", "75000"), false), "application/json").statusCode());
        var updated = post("actions", new ActionRequest("integration", action, Map.of("salary", "75000"), true), "application/json");
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals(75000, mapper.readTree(updated.body()).get("results").get(0).get("salary").asInt());
        when(planner.plan(anyString(), anyString(), anyString())).thenReturn(new Plan(List.of(new Step(Operation.LIST, null, Map.of()), new Step(Operation.ANALYTICS, null, Map.of())), null));
        var stream = post("chat", new Request("integration", "dashboard-1", "List employees and show dashboard"), "text/event-stream");
        assertEquals(200, stream.statusCode(), stream.body());
        assertTrue(stream.body().contains("event:agent_started"));
        assertTrue(stream.body().contains("event:ui_ready"));
        assertTrue(stream.body().contains("event:workflow_completed"));

    }
}
