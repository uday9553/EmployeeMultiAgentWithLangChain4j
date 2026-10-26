package com.uday.sample.chat;

import com.uday.sample.service.*;
import org.springframework.stereotype.Component;
import dev.langchain4j.agentic.Agent;

import java.util.*;

@Component
public class AnalyticsAgent {
    private final EmployeeService employees;
    private final PromotionService promotions;

    public AnalyticsAgent(EmployeeService employees, PromotionService promotions) {
        this.employees = employees;
        this.promotions = promotions;
    }

    @Agent(description = "Returns verified promotion-eligible employees")
    public List<PromotionService.Assessment> eligible() {
        return employees.list().stream().map(e -> promotions.assess(e.getEmployeeId()))
                .filter(a -> Boolean.TRUE.equals(a.eligible())).toList();
    }

    @Agent(description = "Calculates department performance and employee statistics deterministically")
    public Map<String, Object> dashboard() {
        var all = employees.list();
        Map<String, Object> departments = new TreeMap<>();
        all.stream().collect(java.util.stream.Collectors.groupingBy(e -> Objects.toString(e.getDepartment(), "Unassigned")))
                .forEach((department, group) -> departments.put(department, Map.of("count", group.size(), "ratedCount", group.stream().filter(e -> e.getPerformanceRating() != null && e.getPerformanceRating() > 0).count(), "averagePerformance", group.stream().filter(e -> e.getPerformanceRating() != null && e.getPerformanceRating() > 0).mapToInt(e -> e.getPerformanceRating()).average().orElse(0))));
        return Map.of("employeeCount", all.size(), "eligibleCount", eligible().size(), "departments", departments);
    }
}
