package com.uday.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.Capability;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class Langchain4JExampleApplicationTests {
    @Autowired
    ChatModel model;
    @Autowired
    com.uday.sample.chat.ConversationService conversations;
    @Autowired
    com.uday.sample.repository.EmployeeRepository employees;

    @Test
    void contextLoads() {
        assertTrue(model.supportedCapabilities().contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA));
    }

    @Test
    void listsEmployeesThroughConfiguredWorkflowWithoutModelCall() {
        var response = conversations.chat(new com.uday.sample.chat.ChatTypes.Request(
                "listing-context-test", "listing-1", "show all employees"));
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS", response.status());
        org.junit.jupiter.api.Assertions.assertEquals("employee-table", response.ui().children().getFirst().type());
    }

    @Test
    void demoSeedIncludesDepartmentAndSalary() {
        for (int number = 1001; number <= 1005; number++) {
            var employee = employees.findByEmployeeId("EMP-" + number).orElseThrow();
            org.junit.jupiter.api.Assertions.assertNotNull(employee.getDepartment(), employee.getEmployeeId());
            org.junit.jupiter.api.Assertions.assertFalse(employee.getDepartment().isBlank(), employee.getEmployeeId());
            org.junit.jupiter.api.Assertions.assertNotNull(employee.getSalary(), employee.getEmployeeId());
            assertTrue(employee.getSalary().signum() > 0, employee.getEmployeeId());
        }
    }

}
