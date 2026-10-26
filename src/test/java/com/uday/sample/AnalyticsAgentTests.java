package com.uday.sample;

import com.uday.sample.chat.AnalyticsAgent;
import com.uday.sample.service.*;
import com.uday.sample.model.Employee;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyticsAgentTests {
    @Test
    void aggregatesOnlyVerifiedRatingsAndEligibility() {
        var employees = mock(EmployeeService.class);
        var promotions = mock(PromotionService.class);
        var rated = new Employee();
        rated.setEmployeeId("rated");
        rated.setDepartment("Engineering");
        rated.setPerformanceRating(4);
        var unknown = new Employee();
        unknown.setEmployeeId("unknown");
        unknown.setDepartment("Engineering");
        when(employees.list()).thenReturn(List.of(rated, unknown));
        when(promotions.assess("rated")).thenReturn(new PromotionService.Assessment("rated", "Rated", true, "Senior", "Policy met"));
        when(promotions.assess("unknown")).thenReturn(new PromotionService.Assessment("unknown", "Unknown", null, null, "Missing data"));
        var agent = new AnalyticsAgent(employees, promotions);
        var dashboard = agent.dashboard();
        assertEquals(2, dashboard.get("employeeCount"));
        assertEquals(1, dashboard.get("eligibleCount"));
        var departments = (Map<?, ?>) dashboard.get("departments");
        var engineering = (Map<?, ?>) departments.get("Engineering");
        assertEquals(1L, engineering.get("ratedCount"));
        assertEquals(4.0, engineering.get("averagePerformance"));
    }
}
