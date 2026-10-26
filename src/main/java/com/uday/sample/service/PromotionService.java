package com.uday.sample.service;

import com.uday.sample.repository.PromotionPolicyRepository;
import org.springframework.stereotype.Service;

@Service
public class PromotionService {
    private final EmployeeService employeeService;
    private final PromotionPolicyRepository repository;

    public PromotionService(EmployeeService employeeService, PromotionPolicyRepository repository) {
        this.employeeService = employeeService;
        this.repository = repository;
    }

    public String checkEligibility(String employeeId) {
        var employee = employeeService.getEmployee(employeeId);
        if (employee.getMonthsInRole() == null || employee.getPerformanceRating() == null)
            return "Employee " + employeeId + ": performance or experience data is missing. Eligibility cannot be determined.";
        var policy = repository.findByDesignation(employee.getDesignation());
        if (policy.isEmpty()) {
            return "Employee " + employeeId + ": no promotion policy found for "
                    + employee.getDesignation() + ". Eligibility cannot be determined.";
        }
        var rule = policy.get();
        boolean eligible = meetsPolicy(employee, rule);
        return "Employee " + employeeId + ": eligible=" + eligible
                + ", current designation=" + employee.getDesignation()
                + ", next designation=" + rule.getNextDesignation()
                + ", months in role=" + employee.getMonthsInRole()
                + ", minimum months=" + rule.getMinimumMonthsInRole()
                + ", performance rating=" + employee.getPerformanceRating()
                + ", minimum rating=" + rule.getMinimumPerformanceRating()
                + ". This is an eligibility check only; no promotion has been applied.";
    }

    public record Assessment(String employeeId, String employeeName, Boolean eligible,
                             String nextDesignation, String explanation) {
    }

    public Assessment assess(String employeeId) {
        var employee = employeeService.getEmployee(employeeId);
        var policy = repository.findByDesignation(employee.getDesignation());
        Boolean eligible = employee.getMonthsInRole() == null || employee.getPerformanceRating() == null ? null : policy.map(rule -> meetsPolicy(employee, rule)).orElse(null);
        return new Assessment(employeeId, employee.getName(), eligible,
                policy.map(p -> p.getNextDesignation()).orElse(null), checkEligibility(employeeId));
    }

    private boolean meetsPolicy(com.uday.sample.model.Employee employee, com.uday.sample.model.PromotionPolicy policy) {
        return employee.getMonthsInRole() >= policy.getMinimumMonthsInRole()
                && employee.getPerformanceRating() >= policy.getMinimumPerformanceRating();
    }
}
