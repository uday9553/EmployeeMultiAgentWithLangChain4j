package com.uday.sample.model;

import jakarta.persistence.*;

@Entity
@Table(name = "promotion_policy")
public class PromotionPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String designation;
    private String nextDesignation;
    private int minimumMonthsInRole;
    private int minimumPerformanceRating;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getNextDesignation() { return nextDesignation; }
    public void setNextDesignation(String nextDesignation) { this.nextDesignation = nextDesignation; }
    public int getMinimumMonthsInRole() { return minimumMonthsInRole; }
    public void setMinimumMonthsInRole(int minimumMonthsInRole) { this.minimumMonthsInRole = minimumMonthsInRole; }
    public int getMinimumPerformanceRating() { return minimumPerformanceRating; }
    public void setMinimumPerformanceRating(int minimumPerformanceRating) { this.minimumPerformanceRating = minimumPerformanceRating; }
}
