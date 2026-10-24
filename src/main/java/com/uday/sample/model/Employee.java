package com.uday.sample.model;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String employeeId;
    private String name;
    private String designation;
    private int monthsInRole;
    private int performanceRating;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public int getMonthsInRole() { return monthsInRole; }
    public void setMonthsInRole(int monthsInRole) { this.monthsInRole = monthsInRole; }
    public int getPerformanceRating() { return performanceRating; }
    public void setPerformanceRating(int performanceRating) { this.performanceRating = performanceRating; }
}
