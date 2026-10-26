package com.uday.sample.chat;

import com.uday.sample.service.EmployeeService;
import com.uday.sample.model.Employee;
import org.springframework.stereotype.Component;
import dev.langchain4j.agentic.Agent;

import java.util.Map;

/**
 * Deterministic employee operations invoked by the orchestrator.
 */
@Component
public class ManagementAgent {
    private final EmployeeService service;

    public ManagementAgent(EmployeeService service) {
        this.service = service;
    }

    @Agent(description = "Creates a validated employee and returns the actual generated ID")
    public Employee create(Map<String, String> fields) {
        return service.create(fields);
    }

    @Agent(description = "Retrieves an employee by unique name or exact employee ID")
    public Employee get(String reference) {
        return service.resolve(reference);
    }

    @Agent(description = "Lists verified employees")
    public java.util.List<Employee> list() {
        return service.list();
    }

    @Agent(description = "Applies a validated employee update")
    public Employee update(String employeeId, Map<String, String> fields) {
        return service.update(employeeId, fields);
    }
}
