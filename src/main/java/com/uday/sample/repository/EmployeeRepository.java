package com.uday.sample.repository;

import com.uday.sample.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String value);

    @org.springframework.data.jpa.repository.Query("select employee.employeeId from Employee employee")
    java.util.List<String> findAllEmployeeIds();
}
