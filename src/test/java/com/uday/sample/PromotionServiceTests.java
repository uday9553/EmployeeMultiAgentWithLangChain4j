package com.uday.sample;

import com.uday.sample.model.Employee;
import com.uday.sample.model.PromotionPolicy;
import com.uday.sample.repository.PromotionPolicyRepository;
import com.uday.sample.service.EmployeeService;
import com.uday.sample.service.PromotionService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PromotionServiceTests {
    @Test
    void checksBothRequirementsIncludingTheirBoundaries() {
        var employees = mock(EmployeeService.class);
        var policies = mock(PromotionPolicyRepository.class);
        var employee = new Employee();
        employee.setDesignation("Developer");
        employee.setMonthsInRole(24);
        employee.setPerformanceRating(4);
        var policy = new PromotionPolicy();
        policy.setNextDesignation("Senior Developer");
        policy.setMinimumMonthsInRole(24);
        policy.setMinimumPerformanceRating(4);
        when(employees.getEmployee("EMP-1001")).thenReturn(employee);
        when(policies.findByDesignation("Developer")).thenReturn(Optional.of(policy));
        var service = new PromotionService(employees, policies);

        assertTrue(service.checkEligibility("EMP-1001").contains("eligible=true"));
        employee.setMonthsInRole(23);
        assertTrue(service.checkEligibility("EMP-1001").contains("eligible=false"));
        employee.setMonthsInRole(24);
        employee.setPerformanceRating(3);
        assertTrue(service.checkEligibility("EMP-1001").contains("eligible=false"));
    }

    @Test
    void reportsMissingPolicyWithoutInventingEligibility() {
        var employees = mock(EmployeeService.class);
        var policies = mock(PromotionPolicyRepository.class);
        var employee = new Employee();
        employee.setDesignation("Director");
        when(employees.getEmployee("EMP-1001")).thenReturn(employee);
        when(policies.findByDesignation("Director")).thenReturn(Optional.empty());

        assertTrue(new PromotionService(employees, policies).checkEligibility("EMP-1001")
                .contains("Eligibility cannot be determined"));
    }
}
