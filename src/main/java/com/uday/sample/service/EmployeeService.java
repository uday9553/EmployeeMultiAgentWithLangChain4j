package com.uday.sample.service;

import com.uday.sample.model.Employee;
import com.uday.sample.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {
    private final EmployeeRepository repository;
    private final EmployeeIdAllocator ids;

    public EmployeeService(EmployeeRepository repository, EmployeeIdAllocator ids) {
        this.repository = repository;
        this.ids = ids;
    }

    public Employee getEmployee(String employeeId) {
        return repository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
    }

    public java.util.List<Employee> list() {
        return repository.findAll();
    }

    public Employee resolve(String reference) {
        var exact = repository.findByEmployeeId(reference);
        if (exact.isPresent()) return exact.get();
        var matches = list().stream().filter(e -> e.getName().toLowerCase(java.util.Locale.ROOT).contains(reference.toLowerCase(java.util.Locale.ROOT))).toList();
        if (matches.size() != 1) throw new IllegalArgumentException("Provide an exact employee ID or unique full name");
        return matches.getFirst();
    }

    @org.springframework.transaction.annotation.Transactional
    public Employee create(java.util.Map<String, String> fields) {
        validate(fields);
        for (String required : java.util.List.of("name", "department", "salary"))
            if (!fields.containsKey(required)) throw new IllegalArgumentException("Required field: " + required);
        var employee = new Employee();
        employee.setEmployeeId(ids.nextId());
        apply(employee, fields);
        return repository.save(employee);
    }

    @org.springframework.transaction.annotation.Transactional
    public Employee update(String id, java.util.Map<String, String> fields) {
        validate(fields);
        var employee = getEmployee(id);
        apply(employee, fields);
        return repository.save(employee);
    }

    private void validate(java.util.Map<String, String> fields) {
        if (fields == null || fields.isEmpty() || fields.size() > 4)
            throw new IllegalArgumentException("Employee fields required");
        fields.forEach((key, value) -> {
            if (!java.util.Set.of("name", "department", "designation", "salary").contains(key)
                    || value == null || value.isBlank() || value.length() > 150)
                throw new IllegalArgumentException("Invalid employee field: " + key);
            if (key.equals("salary")) {
                var salary = new java.math.BigDecimal(value);
                if (salary.signum() < 0 || salary.scale() > 2 || salary.compareTo(new java.math.BigDecimal("1000000000")) > 0)
                    throw new IllegalArgumentException("Invalid salary");
            }
        });
    }

    public void validateChanges(java.util.Map<String, String> fields) {
        validate(fields);
    }

    private void apply(Employee employee, java.util.Map<String, String> fields) {
        fields.forEach((key, value) -> {
            switch (key) {
                case "name" -> employee.setName(value);
                case "department" -> employee.setDepartment(value);
                case "designation" -> employee.setDesignation(value);
                case "salary" -> employee.setSalary(new java.math.BigDecimal(value));
                default -> throw new IllegalArgumentException("Unsupported field");
            }
        });
    }
}
