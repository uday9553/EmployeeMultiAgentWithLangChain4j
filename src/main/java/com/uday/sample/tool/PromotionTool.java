package com.uday.sample.tool;

import com.uday.sample.service.PromotionService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

@Service
public class PromotionTool {
    private final PromotionService service;

    public PromotionTool(PromotionService service) { this.service = service; }

    @Tool("Check promotion eligibility using an employee ID such as EMP-1001. Uses stored employee data and promotion policy.")
    public String checkPromotionEligibility(String employeeId) {
        return service.checkEligibility(employeeId);
    }
}
