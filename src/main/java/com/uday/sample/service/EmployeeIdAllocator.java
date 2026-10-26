package com.uday.sample.service;

import com.uday.sample.repository.EmployeeIdCounterRepository;
import com.uday.sample.repository.EmployeeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EmployeeIdAllocator {
    private final EmployeeIdCounterRepository counters;
    private final EmployeeRepository employees;
    private final JdbcTemplate jdbc;

    public EmployeeIdAllocator(EmployeeIdCounterRepository counters, EmployeeRepository employees, JdbcTemplate jdbc) {
        this.counters = counters;
        this.employees = employees;
        this.jdbc = jdbc;
    }

    @PostConstruct
    void initialize() {
        // Insert once, in an independent JDBC transaction. Concurrent application startup is safe:
        // the primary key decides who inserts; an existing counter is never reset.
        try {
            jdbc.update("insert into employee_id_counter (id, nextValue) values (1, 1000)");
        } catch (DuplicateKeyException alreadyInitialized) {
            // The persistent database or another application instance already created the row.
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextId() {
        var counter = counters.lockCounter().orElseThrow(() -> new IllegalStateException("Employee ID counter missing"));
        long next = counter.getNextValue();
        if (next == 1000) {
            // Bootstrap above existing numeric IDs; legacy UUID IDs do not participate.
            for (String id : employees.findAllEmployeeIds()) {
                if (id.matches("EMP-[0-9]+")) {
                    long existing = Long.parseLong(id.substring(4));
                    next = Math.max(next, Math.addExact(existing, 1));
                }
            }
        }
        counter.setNextValue(Math.addExact(next, 1));
        // Dirty checking commits the counter with the employee; the lock lasts until that commit.
        return "EMP-" + next;
    }
}
