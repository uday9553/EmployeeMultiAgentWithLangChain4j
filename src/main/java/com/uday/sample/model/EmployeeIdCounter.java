package com.uday.sample.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee_id_counter")
public class EmployeeIdCounter {
    @Id
    private Long id;
    private long nextValue;

    public long getNextValue() {
        return nextValue;
    }

    public void setNextValue(long nextValue) {
        this.nextValue = nextValue;
    }
}
