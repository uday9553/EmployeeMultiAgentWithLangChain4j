package com.uday.sample.controller;

import com.uday.sample.agents.EmployeeWorkflowAgent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-support/employee")
public class EmployeePromotionController {
    private final EmployeeWorkflowAgent workflow;

    public EmployeePromotionController(EmployeeWorkflowAgent workflow) {
        this.workflow = workflow;
    }

    @GetMapping("/promotion")
    public String checkPromotion(@RequestParam String request) {
        return workflow.handle(request);
    }
}
