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
        var policy = repository.findByDesignation(employee.getDesignation());
        if (policy.isEmpty()) {
            return "Employee " + employeeId + ": no promotion policy found for "
                    + employee.getDesignation() + ". Eligibility cannot be determined.";
        }
        var rule = policy.get();
        boolean eligible = employee.getMonthsInRole() >= rule.getMinimumMonthsInRole()
                && employee.getPerformanceRating() >= rule.getMinimumPerformanceRating();
        return "Employee " + employeeId + ": eligible=" + eligible
                + ", current designation=" + employee.getDesignation()
                + ", next designation=" + rule.getNextDesignation()
                + ", months in role=" + employee.getMonthsInRole()
                + ", minimum months=" + rule.getMinimumMonthsInRole()
                + ", performance rating=" + employee.getPerformanceRating()
                + ", minimum rating=" + rule.getMinimumPerformanceRating()
                + ". This is an eligibility check only; no promotion has been applied.";
    }
}
