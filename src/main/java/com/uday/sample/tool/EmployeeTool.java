package com.uday.sample.tool;

import com.uday.sample.model.Employee;
import com.uday.sample.service.EmployeeService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

@Service
public class EmployeeTool {
    private final EmployeeService service;

    public EmployeeTool(EmployeeService service) { this.service = service; }

    @Tool("Fetch employee details using the full employee ID such as EMP-1001, not the numeric database ID")
    public Employee getEmployee(String employeeId) {
        return service.getEmployee(employeeId);
    }
}
